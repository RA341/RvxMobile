package dev.radn.rvxmobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.radn.rvxmobile.data.ApkInfo
import dev.radn.rvxmobile.data.PinnedReleaseAsset
import dev.radn.rvxmobile.ui.DownloadStatus
import dev.radn.rvxmobile.ui.ReadmeViewModel
import dev.radn.rvxmobile.ui.ReleasesState
import dev.radn.rvxmobile.ui.UiState
import dev.radn.rvxmobile.ui.navigation.Screen
import dev.radn.rvxmobile.ui.screens.DashboardScreen
import dev.radn.rvxmobile.ui.screens.DownloadsScreen
import dev.radn.rvxmobile.ui.screens.PinnedScreen
import dev.radn.rvxmobile.ui.screens.ReadmeWebViewScreen
import dev.radn.rvxmobile.ui.screens.SettingsScreen
import dev.radn.rvxmobile.ui.theme.RvxMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RvxMobileTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ReadmeViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val downloadQueue by viewModel.downloadQueue.collectAsState()
    val pinnedVariants by viewModel.pinnedVariants.collectAsState()
    val pinnedReleases by viewModel.pinnedReleases.collectAsState()
    val releasesState by viewModel.releasesState.collectAsState()
    val lastInstalled by viewModel.lastInstalled.collectAsState()
    
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingApksToDownload by remember { mutableStateOf<List<Pair<String, ApkInfo>>>(emptyList()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Register Notification Permission Launcher (for Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            if (pendingApksToDownload.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !context.packageManager.canRequestPackageInstalls()
                ) {
                    showPermissionDialog = true
                } else {
                    pendingApksToDownload.forEach { (patcherName, apk) ->
                        val label = "$patcherName - ${apk.label}"
                        viewModel.enqueueDownload(context, label, apk)
                    }
                    val count = pendingApksToDownload.size
                    pendingApksToDownload = emptyList()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(if (count == 1) "Added to download queue" else "Added $count items to download queue")
                    }
                }
            }
        }
    )

    // Listen to ViewModel install events
    LaunchedEffect(viewModel, downloadQueue) {
        viewModel.installEvent.collect { file ->
            viewModel.installApk(context, file)
            val task = downloadQueue.find { it.apkUrl.substringAfterLast("/") == file.name }
            if (task != null) {
                viewModel.recordInstallation(task.apkUrl)
            }
        }
    }

    // Sync download queue with cache when screen changes to DOWNLOADS or SETTINGS
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.DOWNLOADS || currentScreen == Screen.SETTINGS) {
            viewModel.syncQueueWithCache()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "RvxMobile Installer",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ABI: ${viewModel.deviceAbi}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (currentScreen == Screen.DASHBOARD) {
                        IconButton(onClick = { viewModel.loadData() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh data"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                val activeDownloadsCount = remember(downloadQueue) {
                    downloadQueue.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
                }
                
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        label = { Text(screen.title) },
                        icon = {
                            Box {
                                Icon(screen.icon, contentDescription = screen.title)
                                if (screen == Screen.DOWNLOADS && activeDownloadsCount > 0) {
                                    Badge(
                                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp)
                                    ) {
                                        Text(activeDownloadsCount.toString())
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Success -> {
                    when (currentScreen) {
                        Screen.DASHBOARD -> {
                            DashboardScreen(
                                apps = state.apps,
                                rawMarkdown = state.rawMarkdown,
                                releasesState = releasesState,
                                downloadQueue = downloadQueue,
                                pinnedReleases = pinnedReleases,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                deviceAbi = viewModel.deviceAbi,
                                pinnedVariants = pinnedVariants,
                                onDownloadClick = { patcherName, apk ->
                                    pendingApksToDownload = listOf(Pair(patcherName, apk))
                                    
                                    // Check Post Notifications Permission on Android 13+ (API 33+)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        // Permission already granted or older system. Check package installation permission.
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                            !context.packageManager.canRequestPackageInstalls()
                                        ) {
                                            showPermissionDialog = true
                                        } else {
                                            val label = "$patcherName - ${apk.label}"
                                            viewModel.enqueueDownload(context, label, apk)
                                            pendingApksToDownload = emptyList()
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Added $label to download queue")
                                            }
                                        }
                                    }
                                },
                                onPinClick = { app, patcher, apk ->
                                    viewModel.togglePin(app.name, patcher.name, apk)
                                },
                                onDownloadReleaseClick = { asset ->
                                     val apkInfo = ApkInfo(
                                         label = asset.name.substringBeforeLast(".apk"),
                                         url = asset.browserDownloadUrl,
                                         isBeta = asset.name.contains("beta", ignoreCase = true),
                                         isLite = asset.name.contains("lite", ignoreCase = true),
                                         isOutdated = false
                                     )
                                     viewModel.enqueueDownload(context, asset.name, apkInfo)
                                     coroutineScope.launch {
                                         snackbarHostState.showSnackbar("Added ${asset.name} to download queue")
                                     }
                                 },
                                 onInstallReleaseClick = { task ->
                                     viewModel.installCachedFile(context, task)
                                 },
                                 onRemoveReleaseClick = { task ->
                                     viewModel.removeDownloadTask(task)
                                 },
                                 onRetryReleaseClick = { task ->
                                     val apkInfo = ApkInfo(
                                         label = task.label,
                                         url = task.apkUrl,
                                         isBeta = task.label.contains("beta", ignoreCase = true),
                                         isLite = task.label.contains("lite", ignoreCase = true),
                                         isOutdated = false
                                     )
                                     viewModel.enqueueDownload(context, task.label, apkInfo)
                                 },
                                 onPinReleaseClick = { asset ->
                                     viewModel.togglePinRelease(asset)
                                 },
                                 onRefreshReleasesClick = {
                                     viewModel.loadReleases()
                                 }
                            )
                        }
                        Screen.PINNED -> {
                            PinnedScreen(
                                pinnedVariants = pinnedVariants,
                                releasesState = releasesState,
                                lastInstalled = lastInstalled,
                                apps = state.apps,
                                deviceAbi = viewModel.deviceAbi,
                                onRefreshClick = {
                                    viewModel.loadData()
                                },
                                onDownloadClick = { appName, patcherName, apk ->
                                    pendingApksToDownload = listOf(Pair(patcherName, apk))
                                    
                                    // Check Post Notifications Permission on Android 13+ (API 33+)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        // Permission already granted or older system. Check package installation permission.
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                            !context.packageManager.canRequestPackageInstalls()
                                        ) {
                                            showPermissionDialog = true
                                        } else {
                                            val label = "$patcherName - ${apk.label}"
                                            viewModel.enqueueDownload(context, label, apk)
                                            pendingApksToDownload = emptyList()
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Added $label to download queue")
                                            }
                                        }
                                    }
                                },
                                onDownloadAllClick = { apks ->
                                    pendingApksToDownload = apks
                                    
                                    // Check Post Notifications Permission on Android 13+ (API 33+)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        // Permission already granted or older system. Check package installation permission.
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                            !context.packageManager.canRequestPackageInstalls()
                                        ) {
                                            showPermissionDialog = true
                                        } else {
                                            apks.forEach { (patcherName, apk) ->
                                                val label = "$patcherName - ${apk.label}"
                                                viewModel.enqueueDownload(context, label, apk)
                                            }
                                            pendingApksToDownload = emptyList()
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Added ${apks.size} items to download queue")
                                            }
                                        }
                                    }
                                },
                                onUnpinClick = { pinned ->
                                    val apk = ApkInfo(
                                        label = pinned.apkLabel,
                                        url = "",
                                        isBeta = pinned.isBeta,
                                        isLite = false,
                                        isOutdated = false
                                    )
                                    viewModel.togglePin(pinned.appName, pinned.patcherName, apk)
                                }
                            )
                        }

                        Screen.DOWNLOADS -> {
                            DownloadsScreen(
                                downloadQueue = downloadQueue,
                                onInstallClick = { task ->
                                    viewModel.installCachedFile(context, task)
                                },
                                lastInstalled = lastInstalled,
                                onClearClick = {
                                    viewModel.clearQueueHistory()
                                },
                                onRetryClick = { task ->
                                     val apk = ApkInfo(label = task.label.substringAfter(" - "), url = task.apkUrl, isBeta = false, isLite = false, isOutdated = false)
                                     viewModel.enqueueDownload(context, task.label, apk)
                                     coroutineScope.launch {
                                         snackbarHostState.showSnackbar("Added ${task.label} to download queue")
                                     }
                                 },
                                 onRemoveClick = { task ->
                                     viewModel.removeDownloadTask(task)
                                 },
                                 releasesState = releasesState,
                                 pinnedReleases = pinnedReleases,
                                 onDownloadPinnedReleaseClick = { pinnedAsset ->
                                     val apkInfo = ApkInfo(
                                         label = pinnedAsset.name.substringBeforeLast(".apk"),
                                         url = pinnedAsset.browserDownloadUrl,
                                         isBeta = pinnedAsset.name.contains("beta", ignoreCase = true),
                                         isLite = pinnedAsset.name.contains("lite", ignoreCase = true),
                                         isOutdated = false
                                     )
                                     viewModel.enqueueDownload(context, pinnedAsset.name, apkInfo)
                                     coroutineScope.launch {
                                         snackbarHostState.showSnackbar("Added ${pinnedAsset.name} to download queue")
                                     }
                                 },
                                 onPinReleaseAssetClick = { pinnedAsset ->
                                     viewModel.togglePinReleaseAsset(pinnedAsset)
                                 },
                                 onRefreshReleasesClick = {
                                     viewModel.loadReleases()
                                 }
                            )
                        }
                        Screen.SETTINGS -> {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Failed to load data.",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadData() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }

    // Permission Alert Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Installation Permission Required") },
            text = {
                Text(
                    "To install the patched APKs, RvxMobile requires the 'Install unknown apps' permission. " +
                    "Please enable it in the next screen."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                                                        if (pendingApksToDownload.isNotEmpty()) {
                                 pendingApksToDownload.forEach { (patcherName, apk) ->
                                     val label = "$patcherName - ${apk.label}"
                                     viewModel.enqueueDownload(context, label, apk)
                                 }
                                 val count = pendingApksToDownload.size
                                 coroutineScope.launch {
                                     snackbarHostState.showSnackbar(if (count == 1) "Added to download queue" else "Added $count items to download queue")
                                 }
                                 pendingApksToDownload = emptyList()
                             }
                        }
                    }
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false 
                    pendingApksToDownload = emptyList()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}