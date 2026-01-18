package com.rahul.githubwallpaper.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rahul.githubwallpaper.data.CachedContributionData
import com.rahul.githubwallpaper.data.Week
import com.rahul.githubwallpaper.data.ContributionDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    
    companion object {
        val USERNAME_KEY = stringPreferencesKey("github_username")
        val TOKEN_KEY = stringPreferencesKey("github_token")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val CACHED_DATA_KEY = stringPreferencesKey("cached_contribution_data")
    }
    
    // Username
    suspend fun saveUsername(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
        }
    }
    
    fun getUsername(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USERNAME_KEY]
        }
    }
    
    // Token
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }
    
    fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }
    
    // Dark Mode
    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    fun getDarkMode(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
    }
    
    // Cached Contribution Data - using org.json
    suspend fun saveCachedData(data: CachedContributionData) {
        val json = JSONObject().apply {
            put("username", data.username)
            put("totalContributions", data.totalContributions)
            put("currentStreak", data.currentStreak)
            put("longestStreak", data.longestStreak)
            put("todayCommits", data.todayCommits)
            put("lastUpdated", data.lastUpdated)
            
            val weeksArray = JSONArray()
            for (week in data.weeks) {
                val weekObj = JSONObject()
                val daysArray = JSONArray()
                for (day in week.contributionDays) {
                    val dayObj = JSONObject().apply {
                        put("date", day.date)
                        put("contributionCount", day.contributionCount)
                        put("level", day.level)
                    }
                    daysArray.put(dayObj)
                }
                weekObj.put("contributionDays", daysArray)
                weeksArray.put(weekObj)
            }
            put("weeks", weeksArray)
        }
        
        context.dataStore.edit { preferences ->
            preferences[CACHED_DATA_KEY] = json.toString()
        }
    }
    
    fun getCachedData(): Flow<CachedContributionData?> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[CACHED_DATA_KEY]
            if (jsonString != null) {
                try {
                    val json = JSONObject(jsonString)
                    val weeksArray = json.getJSONArray("weeks")
                    val weeks = mutableListOf<Week>()
                    
                    for (i in 0 until weeksArray.length()) {
                        val weekObj = weeksArray.getJSONObject(i)
                        val daysArray = weekObj.getJSONArray("contributionDays")
                        val days = mutableListOf<ContributionDay>()
                        
                        for (j in 0 until daysArray.length()) {
                            val dayObj = daysArray.getJSONObject(j)
                            days.add(
                                ContributionDay(
                                    date = dayObj.getString("date"),
                                    contributionCount = dayObj.getInt("contributionCount"),
                                    level = dayObj.getString("level")
                                )
                            )
                        }
                        weeks.add(Week(days))
                    }
                    
                    CachedContributionData(
                        username = json.getString("username"),
                        totalContributions = json.getInt("totalContributions"),
                        weeks = weeks,
                        currentStreak = json.getInt("currentStreak"),
                        longestStreak = json.getInt("longestStreak"),
                        todayCommits = json.getInt("todayCommits"),
                        lastUpdated = json.getLong("lastUpdated")
                    )
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
}
