package com.rahul.githubwallpaper.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.rahul.githubwallpaper.data.GitHubRepository
import com.rahul.githubwallpaper.storage.UserPreferences
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class RefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val repository = GitHubRepository(applicationContext)
    private val preferences = UserPreferences(applicationContext)
    
    override suspend fun doWork(): Result {
        return try {
            // Get username and token from preferences
            val username = preferences.getUsername().first()
            val token = preferences.getToken().first()
            
            if (username.isNullOrEmpty()) {
                Log.d(TAG, "No username, skipping")
                return Result.success()
            }
            
            Log.d(TAG, "Refreshing for: $username")
            
            // Fetch fresh contribution data with token
            val result = repository.fetchContributionData(username, token)
            
            if (result.isSuccess) {
                // Broadcast to wallpaper to refresh
                val intent = Intent("com.rahul.githubwallpaper.REFRESH_WALLPAPER")
                applicationContext.sendBroadcast(intent)
                Log.d(TAG, "Refresh successful")
                Result.success()
            } else {
                Log.w(TAG, "Refresh failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during refresh", e)
            Result.retry()
        }
    }
    
    companion object {
        private const val TAG = "RefreshWorker"
        private const val WORK_NAME = "GitHubRefreshWork"
        
        fun scheduleWork(context: Context) {
            val currentTime = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 5)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                
                if (timeInMillis <= currentTime) {
                    add(java.util.Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            val delay = calendar.timeInMillis - currentTime
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<RefreshWorker>(
                24, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
