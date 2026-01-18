package com.rahul.githubwallpaper.data

import android.content.Context
import android.util.Log
import com.rahul.githubwallpaper.storage.UserPreferences
import com.rahul.githubwallpaper.utils.DateUtils
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class GitHubRepository(private val context: Context) {
    
    private val prefs = UserPreferences(context)
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
    
    private val api = retrofit.create(GitHubApi::class.java)
    
    suspend fun fetchContributionData(username: String, token: String?): Result<CachedContributionData> {
        return try {
            Log.d("GitHubRepo", "Fetching via GraphQL for: $username")
            
            // Build GraphQL query exactly as specified
            val graphqlQuery = """
                {
                  user(login: "$username") {
                    contributionsCollection {
                      contributionCalendar {
                        totalContributions
                        weeks {
                          contributionDays {
                            date
                            contributionCount
                            color
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent().replace("\n", " ").replace(Regex("\\s+"), " ")
            
            val requestBody = JSONObject().apply {
                put("query", graphqlQuery)
            }.toString()
            
            // Use token if provided, otherwise try without (will have rate limits)
            val authHeader = if (token.isNullOrBlank()) {
                "" // No auth
            } else {
                "Bearer $token"
            }
            
            Log.d("GitHubRepo", "Using auth: ${if (authHeader.isEmpty()) "NO TOKEN" else "WITH TOKEN"}")
            
            val response = api.executeQuery(authHeader, requestBody)
            
            if (!response.isSuccessful || response.body() == null) {
                Log.e("GitHubRepo", "Failed: ${response.code()} ${response.message()}")
                return loadFromCacheOrFail("HTTP ${response.code()}: ${response.message()}")
            }
            
            val jsonResponse = JSONObject(response.body()!!)
            Log.d("GitHubRepo", "Response received, parsing...")
            
            // Check for errors
            if (jsonResponse.has("errors")) {
                val errors = jsonResponse.getJSONArray("errors")
                val errorMsg = errors.getJSONObject(0).getString("message")
                Log.e("GitHubRepo", "GraphQL error: $errorMsg")
                return loadFromCacheOrFail(errorMsg)
            }
            
            // Parse response
            val data = jsonResponse.getJSONObject("data")
            if (!data.has("user") || data.isNull("user")) {
                return loadFromCacheOrFail("User not found: $username")
            }
            
            val calendar = data
                .getJSONObject("user")
                .getJSONObject("contributionsCollection")
                .getJSONObject("contributionCalendar")
            
            val totalContributions = calendar.getInt("totalContributions")
            val weeksArray = calendar.getJSONArray("weeks")
            
            val weeks = mutableListOf<Week>()
            for (i in 0 until weeksArray.length()) {
                val weekObj = weeksArray.getJSONObject(i)
                val daysArray = weekObj.getJSONArray("contributionDays")
                
                val days = mutableListOf<ContributionDay>()
                for (j in 0 until daysArray.length()) {
                    val dayObj = daysArray.getJSONObject(j)
                    val date = dayObj.getString("date")
                    val count = dayObj.getInt("contributionCount")
                    val color = dayObj.getString("color")
                    
                    // Map color to level
                    val level = when (color) {
                        "#ebedf0", "#161b22" -> "NONE"  // Light/dark theme empty
                        "#9be9a8", "#0e4429" -> "FIRST_QUARTILE"
                        "#40c463", "#006d32" -> "SECOND_QUARTILE"
                        "#30a14e", "#26a641" -> "THIRD_QUARTILE"
                        "#216e39", "#39d353" -> "FOURTH_QUARTILE"
                        else -> if (count == 0) "NONE" else "FOURTH_QUARTILE"
                    }
                    
                    days.add(ContributionDay(date, count, level))
                }
                weeks.add(Week(days))
            }
            
            Log.d("GitHubRepo", "✅ SUCCESS! Total: $totalContributions contributions, ${weeks.size} weeks")
            
            val stats = calculateStats(weeks)
            Log.d("GitHubRepo", "Stats: current=${stats.currentStreak}, longest=${stats.longestStreak}, today=${stats.todayCommits}")
            
            val cachedData = CachedContributionData(
                username = username,
                totalContributions = totalContributions,
                weeks = weeks,
                currentStreak = stats.currentStreak,
                longestStreak = stats.longestStreak,
                todayCommits = stats.todayCommits,
                lastUpdated = System.currentTimeMillis()
            )
            
            prefs.saveCachedData(cachedData)
            
            Result.success(cachedData)
        } catch (e: Exception) {
            Log.e("GitHubRepo", "Error: ${e.message}", e)
            loadFromCacheOrFail(e.message ?: e.toString())
        }
    }
    
    private suspend fun loadFromCacheOrFail(errorMsg: String): Result<CachedContributionData> {
        val cached = prefs.getCachedData().first()
        return if (cached != null) {
            Log.d("GitHubRepo", "Using cached data")
            Result.success(cached)
        } else {
            Result.failure(Exception(errorMsg))
        }
    }
    
    private fun calculateStats(weeks: List<Week>): ContributionStats {
        val allDays = weeks.flatMap { it.contributionDays }.sortedBy { it.date }
        
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var todayCommits = 0
        
        val today = DateUtils.getTodayDate()
        
        todayCommits = allDays.find { it.date == today }?.contributionCount ?: 0
        
        var foundToday = false
        for (day in allDays.reversed()) {
            if (day.date == today) {
                foundToday = true
            }
            
            if (foundToday) {
                if (day.contributionCount > 0) {
                    currentStreak++
                } else {
                    break
                }
            }
        }
        
        for (day in allDays) {
            if (day.contributionCount > 0) {
                tempStreak++
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                tempStreak = 0
            }
        }
        
        return ContributionStats(currentStreak, longestStreak, todayCommits)
    }
    
    suspend fun getCachedData(): CachedContributionData? {
        return prefs.getCachedData().first()
    }
}

data class ContributionStats(
    val currentStreak: Int,
    val longestStreak: Int,
    val todayCommits: Int
)
