package dev.radn.rvxmobile.ui.utils

import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    fun formatDate(isoDate: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = inputFormat.parse(isoDate) ?: return isoDate
            
            val diff = System.currentTimeMillis() - date.time
            getRelativeTimeSpan(diff)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatLastInstalled(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return try {
            val diff = System.currentTimeMillis() - timestamp
            getRelativeTimeSpan(diff)
        } catch (e: Exception) {
            ""
        }
    }

    fun getFullDate(isoDate: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = inputFormat.parse(isoDate) ?: return isoDate
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun getFullLastInstalled(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return try {
            val date = java.util.Date(timestamp)
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    private fun getRelativeTimeSpan(diff: Long): String {
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            diff < 0 -> "just now"
            seconds < 60 -> "just now"
            minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
            days < 7 -> if (days == 1L) "1 day ago" else "$days days ago"
            weeks < 4 -> if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
            months < 12 -> if (months == 1L) "1 month ago" else "$months months ago"
            else -> if (years == 1L) "1 year ago" else "$years years ago"
        }
    }
}
