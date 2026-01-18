package com.rahul.githubwallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.rahul.githubwallpaper.data.GitHubRepository
import com.rahul.githubwallpaper.storage.UserPreferences
import com.rahul.githubwallpaper.wallpaper.GitHubWallpaperService
import com.rahul.githubwallpaper.worker.RefreshWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    
    private lateinit var repository: GitHubRepository
    private lateinit var preferences: UserPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = GitHubRepository(this)
        preferences = UserPreferences(this)
        
        // Schedule daily refresh worker
        RefreshWorker.scheduleWork(this)
        
        setContent {
            GitHubWallpaperTheme {
                MainScreen()
            }
        }
    }
    
    @Composable
    fun GitHubWallpaperTheme(content: @Composable () -> Unit) {
        var isDarkMode by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            isDarkMode = preferences.getDarkMode().first()
        }
        
        MaterialTheme(
            colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme(),
            content = content
        )
    }
    
    @Composable
    fun MainScreen() {
        var username by remember { mutableStateOf("") }
        var token by remember { mutableStateOf("") }
        var isDarkMode by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var currentStreak by remember { mutableStateOf(0) }
        var longestStreak by remember { mutableStateOf(0) }
        var todayCommits by remember { mutableStateOf(0) }
        
        // Load saved data
        LaunchedEffect(Unit) {
            username = preferences.getUsername().first() ?: ""
            token = preferences.getToken().first() ?: ""
            isDarkMode = preferences.getDarkMode().first()
            
            val cachedData = preferences.getCachedData().first()
            if (cachedData != null) {
                currentStreak = cachedData.currentStreak
                longestStreak = cachedData.longestStreak
                todayCommits = cachedData.todayCommits
            }
        }
        
        val backgroundColor = if (isDarkMode) Color(0xFF0D1117) else Color.White
        val textColor = if (isDarkMode) Color(0xFFC9D1D9) else Color(0xFF24292F)
        val cardColor = if (isDarkMode) Color(0xFF161B22) else Color(0xFFF6F8FA)
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = "GitHub Wallpaper",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Your contribution graph as live wallpaper",
                    fontSize = 14.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Username Input
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("GitHub Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Token Input (optional)
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("GitHub Token (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("ghp_xxxxx...") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Dark Mode Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dark Mode",
                            fontSize = 16.sp,
                            color = textColor
                        )
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { checked ->
                                isDarkMode = checked
                                lifecycleScope.launch {
                                    preferences.saveDarkMode(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Color(0xFF39D353),
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats Card
                if (currentStreak > 0 || longestStreak > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            StatRow("🔥 Current Streak", "$currentStreak days", textColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StatRow("⭐ Longest Streak", "$longestStreak days", textColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            StatRow("📝 Today", "$todayCommits commits", textColor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Error Message
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Set Wallpaper Button
                Button(
                    onClick = {
                        if (username.isEmpty()) {
                            errorMessage = "Please enter a GitHub username"
                            return@Button
                        }
                        
                        lifecycleScope.launch {
                            isLoading = true
                            errorMessage = ""
                            
                            try {
                                // Save username and token
                                preferences.saveUsername(username)
                                if (token.isNotBlank()) {
                                    preferences.saveToken(token)
                                }
                                
                                // Fetch data with token
                                val result = repository.fetchContributionData(username, token.ifBlank { null })
                                
                                if (result.isSuccess) {
                                    val data = result.getOrNull()
                                    if (data != null) {
                                        currentStreak = data.currentStreak
                                        longestStreak = data.longestStreak
                                        todayCommits = data.todayCommits
                                    }
                                    
                                    // Open wallpaper picker
                                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                                    intent.putExtra(
                                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                        ComponentName(
                                            this@MainActivity,
                                            GitHubWallpaperService::class.java
                                        )
                                    )
                                    startActivity(intent)
                                } else {
                                    errorMessage = "Failed to fetch contribution data. ${result.exceptionOrNull()?.message ?: "Check username."}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message ?: e.toString()}"
                                e.printStackTrace()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF39D353),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Set as Wallpaper",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Updates daily at 12:05 AM",
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.5f)
                )
            }
        }
    }
    
    @Composable
    fun StatRow(label: String, value: String, textColor: Color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = textColor
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}
