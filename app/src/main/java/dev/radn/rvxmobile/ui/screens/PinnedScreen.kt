package dev.radn.rvxmobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.radn.rvxmobile.data.ApkInfo
import dev.radn.rvxmobile.data.AppInfo
import dev.radn.rvxmobile.data.PinnedVariant
import dev.radn.rvxmobile.ui.ReleasesState
import java.util.Locale
import java.util.TimeZone

@Composable
fun PinnedScreen(
    pinnedVariants: List<PinnedVariant>,
    releasesState: ReleasesState,
    lastInstalled: Map<String, Long>,
    apps: List<AppInfo>,
    deviceAbi: String,
    onRefreshClick: () -> Unit,
    onDownloadClick: (String, String, ApkInfo) -> Unit,
    onDownloadAllClick: (List<Pair<String, ApkInfo>>) -> Unit,
    onUnpinClick: (PinnedVariant) -> Unit
) {
    val localContext = LocalContext.current
    val releaseAssets = remember(releasesState) {
        (releasesState as? ReleasesState.Success)?.release?.assets ?: emptyList()
    }

    if (pinnedVariants.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "No Pins",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Pinned Variants",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Go to the Installer tab, choose an app, and click the bookmark icon next to your desired variant to pin it here.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Your Bookmarks",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "Quick access to your pinned releases.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                        
                        val availablePins = remember(pinnedVariants, apps) {
                            pinnedVariants.mapNotNull { pinned ->
                                val matchedApp = apps.find { it.name.equals(pinned.appName, ignoreCase = true) }
                                val matchedPatcher = matchedApp?.patchers?.find { it.name.equals(pinned.patcherName, ignoreCase = true) }
                                val liveApk = matchedPatcher?.apks?.find { 
                                    it.label.trim() == pinned.apkLabel.trim() && it.isBeta == pinned.isBeta 
                                } ?: matchedPatcher?.apks?.find { 
                                    it.label.trim() == pinned.apkLabel.trim() 
                                }
                                if (liveApk != null) Pair(pinned.patcherName, liveApk) else null
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onRefreshClick) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Check for updates"
                                )
                            }
                            if (availablePins.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onDownloadAllClick(availablePins) },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier.height(36.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Install All",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Install All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            items(pinnedVariants) { pinned ->
                // Resolve live details from current feed
                val matchedApp = apps.find { it.name.equals(pinned.appName, ignoreCase = true) }
                val matchedPatcher = matchedApp?.patchers?.find { it.name.equals(pinned.patcherName, ignoreCase = true) }
                val liveApk = matchedPatcher?.apks?.find { 
                    it.label.trim() == pinned.apkLabel.trim() && it.isBeta == pinned.isBeta 
                } ?: matchedPatcher?.apks?.find { 
                    it.label.trim() == pinned.apkLabel.trim() 
                }

                val matchedAsset = remember(liveApk, releaseAssets) {
                    liveApk?.let { apk ->
                        releaseAssets.find { resolveUrl(apk.url) == it.browserDownloadUrl }
                    }
                }

                val lastInstalledTime = remember(liveApk, lastInstalled) {
                    liveApk?.let { apk ->
                        lastInstalled[resolveUrl(apk.url)] ?: 0L
                    } ?: 0L
                }

                val isRecommended = remember(pinned.apkLabel, deviceAbi) {
                    val labelLower = pinned.apkLabel.lowercase()
                    val deviceAbiLower = deviceAbi.lowercase()
                    deviceAbiLower in labelLower || 
                            labelLower.contains("all architectures") || 
                            labelLower.contains("all-architectures") ||
                            (deviceAbiLower.contains("64") && labelLower.contains("64"))
                }

                val isOutdated = liveApk?.isOutdated ?: false
                val isLite = liveApk?.isLite ?: false

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Header: App name and Bookmark icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pinned.appName.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = pinned.appName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = pinned.patcherName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = { onUnpinClick(pinned) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Unpin",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Variant Info & Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Variant: ${pinned.apkLabel}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (matchedAsset != null) {
                                    Text(
                                        text = "Size: ${formatSize(matchedAsset.size)} | Downloads: ${matchedAsset.downloadCount}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Updated: ${formatDate(matchedAsset.updatedAt)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.pointerInput(matchedAsset.updatedAt) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    Toast.makeText(localContext, "Full Date: ${getFullDate(matchedAsset.updatedAt)}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    )
                                }
                                if (lastInstalledTime > 0L) {
                                    Text(
                                        text = "Last Installed: ${formatLastInstalled(lastInstalledTime)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.pointerInput(lastInstalledTime) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    Toast.makeText(localContext, "Installed: ${getFullLastInstalled(lastInstalledTime)}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // Badges
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (liveApk == null) {
                                        // Removed Badge
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.errorContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Removed from Source",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    } else {
                                        // Stable/Beta Badge
                                        val badgeColor = if (pinned.isBeta) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = badgeColor.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (pinned.isBeta) "Beta" else "Stable",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor
                                            )
                                        }

                                        if (isLite) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF2E7D32).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Lite", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (isOutdated) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFC62828).copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Outdated", fontSize = 9.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    if (isRecommended) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Matches ABI",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Download action
                            Button(
                                onClick = {
                                    if (liveApk != null) {
                                        onDownloadClick(pinned.appName, pinned.patcherName, liveApk)
                                    }
                                },
                                enabled = liveApk != null,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun resolveUrl(url: String): String {
    return if (url.startsWith("http")) {
        url
    } else {
        val cleanPath = url.substringAfter("releases/download/")
        "https://github.com/FiorenMas/Revanced-And-Revanced-Extended-Non-Root/releases/download/$cleanPath"
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> String.format(Locale.getDefault(), "%.2f KB", bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = inputFormat.parse(isoDate) ?: return isoDate
        
        val diff = System.currentTimeMillis() - date.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        val relativeTime = when {
            diff < 0 -> "just now"
            seconds < 60 -> "just now"
            minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
            days < 7 -> if (days == 1L) "1 day ago" else "$days days ago"
            weeks < 4 -> if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
            months < 12 -> if (months == 1L) "1 month ago" else "$months months ago"
            else -> if (years == 1L) "1 year ago" else "$years years ago"
        }
        
        relativeTime
    } catch (e: Exception) {
        isoDate
    }
}

private fun formatLastInstalled(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        when {
            diff < 0 -> "just now"
            seconds < 60 -> "just now"
            minutes < 60 -> if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            hours < 24 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
            days < 7 -> if (days == 1L) "1 day ago" else "$days days ago"
            weeks < 4 -> if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
            months < 12 -> if (months == 1L) "1 month ago" else "$months months ago"
            else -> if (years == 1L) "1 year ago" else "$years years ago"
        }
    } catch (e: Exception) {
        ""
    }
}

private fun getFullDate(isoDate: String): String {
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

private fun getFullLastInstalled(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    return try {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.format(date)
    } catch (e: Exception) {
        ""
    }
}
