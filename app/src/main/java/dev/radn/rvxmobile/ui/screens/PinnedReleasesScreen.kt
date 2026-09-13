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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.radn.rvxmobile.data.PinnedReleaseAsset
import dev.radn.rvxmobile.data.ReleaseAsset
import dev.radn.rvxmobile.ui.DownloadStatus
import dev.radn.rvxmobile.ui.DownloadTask
import dev.radn.rvxmobile.ui.ReleasesState
import java.util.Locale
import java.util.TimeZone

@Composable
fun PinnedReleasesScreen(
    pinnedReleases: List<PinnedReleaseAsset>,
    downloadQueue: List<DownloadTask>,
    releasesState: ReleasesState,
    lastInstalled: Map<String, Long>,
    onDownloadClick: (PinnedReleaseAsset) -> Unit,
    onInstallClick: (DownloadTask) -> Unit,
    onRemoveClick: (DownloadTask) -> Unit,
    onRetryClick: (DownloadTask) -> Unit,
    onPinClick: (PinnedReleaseAsset) -> Unit,
    onRefreshClick: () -> Unit
) {
    val liveAssets = remember(releasesState) {
        (releasesState as? ReleasesState.Success)?.release?.assets ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pinned Releases",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Quick access to your pinned pre-patched releases",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Check for updates"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pinnedReleases.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "No Pinned Releases",
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No pinned releases",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bookmark release assets in the Releases tab to see them here",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pinnedReleases) { asset ->
                    // Find if there is a matching live task
                    val task = downloadQueue.find { it.apkUrl == asset.browserDownloadUrl }
                    
                    // Check for updates
                    val pinnedPrefix = getReleaseAssetPrefix(asset.name)
                    val latestLiveAsset = liveAssets.find { getReleaseAssetPrefix(it.name) == pinnedPrefix }
                        ?: liveAssets.find { it.browserDownloadUrl == asset.browserDownloadUrl }
                        
                    val hasUpdate = latestLiveAsset != null && isNewer(latestLiveAsset.updatedAt, asset.updatedAt)
                    
                    // Get last installed time
                    val lastInstalledTime = maxOf(
                        lastInstalled[asset.browserDownloadUrl] ?: 0L,
                        latestLiveAsset?.let { lastInstalled[it.browserDownloadUrl] } ?: 0L
                    )

                    PinnedReleaseAssetCard(
                        asset = asset,
                        task = task,
                        hasUpdate = hasUpdate,
                        latestLiveAsset = latestLiveAsset,
                        lastInstalledTime = lastInstalledTime,
                        onPinClick = { onPinClick(asset) },
                        onDownloadClick = onDownloadClick,
                        onInstallClick = onInstallClick,
                        onRemoveClick = onRemoveClick,
                        onRetryClick = onRetryClick
                    )
                }
            }
        }
    }
}

@Composable
fun PinnedReleaseAssetCard(
    asset: PinnedReleaseAsset,
    task: DownloadTask?,
    hasUpdate: Boolean,
    latestLiveAsset: ReleaseAsset?,
    lastInstalledTime: Long,
    onPinClick: () -> Unit,
    onDownloadClick: (PinnedReleaseAsset) -> Unit,
    onInstallClick: (DownloadTask) -> Unit,
    onRemoveClick: (DownloadTask) -> Unit,
    onRetryClick: (DownloadTask) -> Unit
) {
    val localContext = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = asset.name.substringBeforeLast(".apk"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Size: ${formatSize(asset.size)} | Downloads: ${asset.downloadCount}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Updated: ${formatDate(asset.updatedAt)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.pointerInput(asset.updatedAt) {
                            detectTapGestures(
                                onLongPress = {
                                    Toast.makeText(localContext, "Full Date: ${getFullDate(asset.updatedAt)}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
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
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasUpdate) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                               )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Update Available",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (task != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (task.status) {
                                        DownloadStatus.QUEUED -> MaterialTheme.colorScheme.surfaceVariant
                                        DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer
                                        DownloadStatus.COMPLETED -> Color(0xFFE8F5E9)
                                        DownloadStatus.FAILED -> Color(0xFFFFEBEE)
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (task.status) {
                                    DownloadStatus.QUEUED -> "Queued"
                                    DownloadStatus.DOWNLOADING -> "Downloading"
                                    DownloadStatus.COMPLETED -> "Completed"
                                    DownloadStatus.FAILED -> "Failed"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (task.status) {
                                    DownloadStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.onPrimaryContainer
                                    DownloadStatus.COMPLETED -> Color(0xFF2E7D32)
                                    DownloadStatus.FAILED -> Color(0xFFC62828)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Choose asset details to download (if update is available, download the latest version details)
            val assetToDownload = if (hasUpdate && latestLiveAsset != null) {
                PinnedReleaseAsset(
                    name = latestLiveAsset.name,
                    size = latestLiveAsset.size,
                    downloadCount = latestLiveAsset.downloadCount,
                    browserDownloadUrl = latestLiveAsset.browserDownloadUrl,
                    updatedAt = latestLiveAsset.updatedAt
                )
            } else {
                asset
            }

            val remoteTimeMillis = remember(asset.updatedAt, latestLiveAsset?.updatedAt) {
                maxOf(
                    parseIsoDateToMillis(asset.updatedAt),
                    parseIsoDateToMillis(latestLiveAsset?.updatedAt ?: "")
                )
            }
            val isUpdateEnabled = remoteTimeMillis > 0L && remoteTimeMillis > lastInstalledTime

            if (task == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPinClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Unpin release",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Button(
                        onClick = { onDownloadClick(assetToDownload) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = if (hasUpdate) "Update" else "Download",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (hasUpdate) "Update" else "Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                when (task.status) {
                    DownloadStatus.DOWNLOADING -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = onPinClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Unpin release",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = { task.progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(task.progress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onRemoveClick(task) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancel",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    DownloadStatus.FAILED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onPinClick,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = "Unpin release",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = task.errorMsg ?: "Download failed",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onRemoveClick(task) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                TextButton(
                                    onClick = { onRetryClick(task) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onPinClick,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = "Unpin release",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Downloaded successfully",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 11.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onRemoveClick(task) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete file",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { onInstallClick(task) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ) {
                                        Text("Install", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onDownloadClick(assetToDownload) },
                                        enabled = isUpdateEnabled,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ) {
                                        Text("Update", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    DownloadStatus.QUEUED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onPinClick,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = "Unpin release",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Waiting for other downloads...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(
                                onClick = { onRemoveClick(task) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancel",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getReleaseAssetPrefix(name: String): String {
    val noExt = name.substringBeforeLast(".apk")
    val versionRegex = Regex("-[vV]\\d+.*")
    return noExt.replace(versionRegex, "")
}

private fun isNewer(liveIsoDate: String, pinnedIsoDate: String): Boolean {
    return try {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val liveDate = format.parse(liveIsoDate) ?: return false
        val pinnedDate = format.parse(pinnedIsoDate) ?: return false
        liveDate.after(pinnedDate)
    } catch (e: Exception) {
        false
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

private fun parseIsoDateToMillis(isoDate: String): Long {
    if (isoDate.isBlank()) return 0L
    return try {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        format.parse(isoDate)?.time ?: 0L
    } catch (e: Exception) {
        try {
            val format2 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format2.parse(isoDate)?.time ?: 0L
        } catch (e2: Exception) {
            0L
        }
    }
}

