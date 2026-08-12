package dev.radn.rvxmobile.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import dev.radn.rvxmobile.data.ApkInfo
import dev.radn.rvxmobile.data.AppInfo
import dev.radn.rvxmobile.data.PatcherInfo
import dev.radn.rvxmobile.data.PinnedReleaseAsset
import dev.radn.rvxmobile.data.PinnedVariant
import dev.radn.rvxmobile.data.ReleaseAsset
import dev.radn.rvxmobile.ui.DownloadTask
import dev.radn.rvxmobile.ui.ReleasesState
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    apps: List<AppInfo>,
    rawMarkdown: String,
    releasesState: ReleasesState,
    downloadQueue: List<DownloadTask>,
    pinnedReleases: List<PinnedReleaseAsset>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    deviceAbi: String,
    pinnedVariants: List<PinnedVariant>,
    onDownloadClick: (String, ApkInfo) -> Unit,
    onPinClick: (AppInfo, PatcherInfo, ApkInfo) -> Unit,
    
    // Releases Actions
    onDownloadReleaseClick: (ReleaseAsset) -> Unit,
    onInstallReleaseClick: (DownloadTask) -> Unit,
    onRemoveReleaseClick: (DownloadTask) -> Unit,
    onRetryReleaseClick: (DownloadTask) -> Unit,
    onPinReleaseClick: (ReleaseAsset) -> Unit,
    onRefreshReleasesClick: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("README", "Releases", "Raw README")

    val filteredApps = remember(apps, searchQuery) {
        apps.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    var selectedAppForDetail by remember { mutableStateOf<AppInfo?>(null) }
    val releaseAssets = remember(releasesState) {
        (releasesState as? ReleasesState.Success)?.release?.assets ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                // Search field
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search apps...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { onSearchQueryChange("") }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            // MicroG warning banner
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "MicroG Alert",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "For YouTube, YouTube Music, and Google Photos, make sure to download and install MicroG RE first.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        items(filteredApps) { app ->
                            AppGridCard(
                                app = app,
                                onClick = { selectedAppForDetail = app }
                            )
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                1 -> {
                    ReleasesScreen(
                        releasesState = releasesState,
                        downloadQueue = downloadQueue,
                        pinnedReleases = pinnedReleases,
                        onDownloadClick = onDownloadReleaseClick,
                        onInstallClick = onInstallReleaseClick,
                        onRemoveClick = onRemoveReleaseClick,
                        onRetryClick = onRetryReleaseClick,
                        onPinClick = onPinReleaseClick,
                        onRefreshClick = onRefreshReleasesClick
                    )
                }
                2 -> {
                    ReadmeWebViewScreen(rawMarkdown = rawMarkdown)
                }
            }
        }
    }

    selectedAppForDetail?.let { app ->
        AppDetailDialog(
            app = app,
            deviceAbi = deviceAbi,
            pinnedVariants = pinnedVariants,
            releaseAssets = releaseAssets,
            onDownloadClick = onDownloadClick,
            onPinClick = onPinClick,
            onDismiss = { selectedAppForDetail = null }
        )
    }
}

@Composable
fun AppGridCard(
    app: AppInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = app.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${app.patchers.size} ${if (app.patchers.size == 1) "variant" else "variants"}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AppDetailDialog(
    app: AppInfo,
    deviceAbi: String,
    pinnedVariants: List<PinnedVariant>,
    releaseAssets: List<ReleaseAsset>,
    onDownloadClick: (String, ApkInfo) -> Unit,
    onPinClick: (AppInfo, PatcherInfo, ApkInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPatcherIndex by remember(app.patchers) { mutableIntStateOf(0) }
    val selectedPatcher = app.patchers.getOrNull(selectedPatcherIndex) ?: app.patchers.firstOrNull()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 550.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (app.patchers.isEmpty()) {
                        Text(
                            text = "No patcher downloads found in raw data.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    } else {
                        if (app.patchers.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                itemsIndexed(app.patchers) { index, patcher ->
                                    val isSelected = index == selectedPatcherIndex
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                            .clickable { selectedPatcherIndex = index }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = patcher.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        selectedPatcher?.let { patcher ->
                            PatcherSection(
                                appName = app.name,
                                patcher = patcher,
                                deviceAbi = deviceAbi,
                                pinnedVariants = pinnedVariants,
                                releaseAssets = releaseAssets,
                                onDownloadClick = { apk -> onDownloadClick(patcher.name, apk) },
                                onPinClick = { apk -> onPinClick(app, patcher, apk) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatcherSection(
    appName: String,
    patcher: PatcherInfo,
    deviceAbi: String,
    pinnedVariants: List<PinnedVariant>,
    releaseAssets: List<ReleaseAsset>,
    onDownloadClick: (ApkInfo) -> Unit,
    onPinClick: (ApkInfo) -> Unit
) {
    val context = LocalContext.current
    
    val isPinned = remember(pinnedVariants, appName, patcher.name) {
        { apk: ApkInfo ->
            pinnedVariants.any {
                it.appName.equals(appName, ignoreCase = true) &&
                it.patcherName.equals(patcher.name, ignoreCase = true) &&
                it.apkLabel == apk.label &&
                it.isBeta == apk.isBeta
            }
        }
    }
    
    val stableApks = remember(patcher.apks) { patcher.apks.filter { !it.isBeta } }
    val betaApks = remember(patcher.apks) { patcher.apks.filter { it.isBeta } }

    fun getRecommendedApk(apks: List<ApkInfo>): ApkInfo? {
        return apks.firstOrNull { apk ->
            val labelLower = apk.label.lowercase()
            val deviceAbiLower = deviceAbi.lowercase()
            val isMatch = deviceAbiLower in labelLower || 
                    labelLower.contains("all architectures") || 
                    labelLower.contains("all-architectures") ||
                    (deviceAbiLower.contains("64") && labelLower.contains("64"))
            isMatch && !apk.isOutdated
        } ?: apks.firstOrNull { apk ->
            val labelLower = apk.label.lowercase()
            val deviceAbiLower = deviceAbi.lowercase()
            deviceAbiLower in labelLower || 
                    labelLower.contains("all architectures") || 
                    labelLower.contains("all-architectures") ||
                    (deviceAbiLower.contains("64") && labelLower.contains("64"))
        } ?: apks.firstOrNull()
    }

    val recommendedStable = remember(stableApks, deviceAbi) { getRecommendedApk(stableApks) }
    val recommendedBeta = remember(betaApks, deviceAbi) { getRecommendedApk(betaApks) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = patcher.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (patcher.githubLink.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, patcher.githubLink.toUri())
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open Patcher GitHub",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        if (patcher.apks.isEmpty()) {
            Text(
                text = "No direct APKs listed. Click the link icon above.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            if (stableApks.isNotEmpty()) {
                ApkGroupDropdown(
                    title = "Stable Releases",
                    titleColor = MaterialTheme.colorScheme.primary,
                    apks = stableApks,
                    recommendedApk = recommendedStable,
                    releaseAssets = releaseAssets,
                    onDownloadClick = onDownloadClick,
                    isPinned = isPinned,
                    onPinClick = onPinClick
                )
            }
            
            if (betaApks.isNotEmpty()) {
                if (stableApks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                ApkGroupDropdown(
                    title = "Beta / Pre-release",
                    titleColor = MaterialTheme.colorScheme.secondary,
                    apks = betaApks,
                    recommendedApk = recommendedBeta,
                    releaseAssets = releaseAssets,
                    onDownloadClick = onDownloadClick,
                    isPinned = isPinned,
                    onPinClick = onPinClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkGroupDropdown(
    title: String,
    titleColor: Color,
    apks: List<ApkInfo>,
    recommendedApk: ApkInfo?,
    releaseAssets: List<ReleaseAsset>,
    onDownloadClick: (ApkInfo) -> Unit,
    isPinned: (ApkInfo) -> Boolean,
    onPinClick: (ApkInfo) -> Unit
) {
    var selectedApk by remember(apks) { mutableStateOf(recommendedApk) }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedApk?.let { 
                    var label = it.label
                    if (it.isLite) label += " [Lite]"
                    if (it.isOutdated) label += " [Outdated]"
                    
                    // Match with releases assets size if loaded
                    val asset = releaseAssets.find { asset -> resolveUrl(it.url) == asset.browserDownloadUrl }
                    if (asset != null) {
                        label += " (${formatSize(asset.size)})"
                    }
                    if (it == recommendedApk) label += " ⭐"
                    label
                } ?: "Select variant...",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                apks.forEach { apk ->
                    val isApkRecommended = apk == recommendedApk
                    val matchedAsset = releaseAssets.find { resolveUrl(apk.url) == it.browserDownloadUrl }
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val sizeLabel = matchedAsset?.let { " (${formatSize(it.size)})" } ?: ""
                                    Text(
                                        text = apk.label + sizeLabel,
                                        fontWeight = if (isApkRecommended) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    Row(
                                        modifier = Modifier.padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (apk.isLite) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("Lite", fontSize = 8.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (apk.isOutdated) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("Outdated", fontSize = 8.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                if (isApkRecommended) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Recommended",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            selectedApk = apk
                            expanded = false
                        }
                    )
                }
            }
        }

        selectedApk?.let { apk ->
            Spacer(modifier = Modifier.height(8.dp))
            
            val isSelectedRecommended = apk == recommendedApk
            val asset = releaseAssets.find { resolveUrl(apk.url) == it.browserDownloadUrl }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelectedRecommended) 1.5.dp else 1.dp,
                    color = if (isSelectedRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Selected: ${apk.label}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (asset != null) {
                            Text(
                                text = "Size: ${formatSize(asset.size)} | Downloads: ${asset.downloadCount}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Updated: ${formatDate(asset.updatedAt)}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        if (isSelectedRecommended) {
                            Text(
                                text = "Matches device ABI architecture",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    val isPinnedVal = isPinned(apk)
                    IconButton(
                        onClick = { onPinClick(apk) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPinnedVal) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isPinnedVal) "Unpin Variant" else "Pin Variant",
                            tint = if (isPinnedVal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Button(
                        onClick = { onDownloadClick(apk) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
        val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val absoluteTime = outputFormat.format(date)
        
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
        
        "$absoluteTime ($relativeTime)"
    } catch (e: Exception) {
        isoDate
    }
}
