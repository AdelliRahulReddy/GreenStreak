package com.rahul.githubwallpaper.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    fun getTodayDate(): String {
        return dateFormat.format(Date())
    }
    
    fun isToday(dateString: String): Boolean {
        return dateString == getTodayDate()
    }
    
    fun formatDate(dateString: String): String {
        return try {
            val date = dateFormat.parse(dateString)
            val displayFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            displayFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
    
    fun getDaysAgo(dateString: String): Int {
        return try {
            val date = dateFormat.parse(dateString)
            val today = Date()
            val diffInMillis = today.time - (date?.time ?: 0)
            (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            0
        }
    }
    
    fun getWeekNumber(dateString: String): Int {
        return try {
            val date = dateFormat.parse(dateString)
            val calendar = Calendar.getInstance()
            calendar.time = date ?: Date()
            calendar.get(Calendar.WEEK_OF_YEAR)
        } catch (e: Exception) {
            0
        }
    }
    
    fun parseDate(dateString: String): Date? {
        return try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}
