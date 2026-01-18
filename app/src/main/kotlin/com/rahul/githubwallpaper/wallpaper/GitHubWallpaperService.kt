package com.rahul.githubwallpaper.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.rahul.githubwallpaper.data.CachedContributionData
import com.rahul.githubwallpaper.storage.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GitHubWallpaperService : WallpaperService() {
    
    override fun onCreateEngine(): Engine {
        return GitHubWallpaperEngine()
    }
    
    inner class GitHubWallpaperEngine : Engine() {
        
        private var width = 0
        private var height = 0
        private var renderer: HeatmapRenderer? = null
        private var cachedData: CachedContributionData? = null
        private var isDarkMode = false
        
        private val preferences = UserPreferences(this@GitHubWallpaperService)
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        
        private val refreshReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.rahul.githubwallpaper.REFRESH_WALLPAPER") {
                    scope.launch {
                        loadData()
                        drawFrame()
                    }
                }
            }
        }
        
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            
            // Register broadcast receiver
            val filter = IntentFilter("com.rahul.githubwallpaper.REFRESH_WALLPAPER")
            this@GitHubWallpaperService.registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            
            // Load initial data
            scope.launch {
                loadData()
            }
            
            // Observe dark mode changes
            scope.launch {
                preferences.getDarkMode().collectLatest { darkMode ->
                    isDarkMode = darkMode
                    renderer = HeatmapRenderer(width, height, isDarkMode)
                    drawFrame()
                }
            }
            
            // Observe data changes
            scope.launch {
                preferences.getCachedData().collectLatest { data ->
                    cachedData = data
                    drawFrame()
                }
            }
        }
        
        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width
            this.height = height
            renderer = HeatmapRenderer(width, height, isDarkMode)
            drawFrame()
        }
        
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                drawFrame()
            }
        }
        
        override fun onDestroy() {
            super.onDestroy()
            try {
                this@GitHubWallpaperService.unregisterReceiver(refreshReceiver)
            } catch (e: Exception) {
                // Receiver already unregistered
            }
            scope.cancel()
        }
        
        private suspend fun loadData() {
            preferences.getCachedData().collect { data ->
                cachedData = data
            }
        }
        
        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderer?.drawHeatmap(canvas, cachedData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
