package dev.radn.rvxmobile.data

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal fun hasPinnedUpdate(updatedAt: String?, lastInstalledTime: Long): Boolean {
    if (updatedAt == null || lastInstalledTime <= 0L) return false

    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
    val position = ParsePosition(0)
    val updatedTime = format.parse(updatedAt, position)?.time ?: return false
    return position.index == updatedAt.length && updatedTime > lastInstalledTime
}
