package dev.radn.rvxmobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.radn.rvxmobile.data.ApkInfo
import dev.radn.rvxmobile.ui.DownloadStatus
import dev.radn.rvxmobile.ui.ReadmeViewModel
import dev.radn.rvxmobile.ui.UiState
import dev.radn.rvxmobile.ui.navigation.Screen
import dev.radn.rvxmobile.ui.screens.DashboardScreen
import dev.radn.rvxmobile.ui.screens.DownloadsScreen
import dev.radn.rvxmobile.ui.screens.ReadmeWebViewScreen
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
    
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingApkToDownload by remember { mutableStateOf<Pair<String, ApkInfo>?>(null) }

    // Listen to ViewModel install events
    LaunchedEffect(viewModel) {
        viewModel.installEvent.collect { file ->
            viewModel.installApk(context, file)
        }
    }

    Scaffold(
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
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val activeDownloadsCount = remember(downloadQueue) {
                    downloadQueue.count { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
                }
                
                Screen.values().forEach { screen ->
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
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                deviceAbi = viewModel.deviceAbi,
                                onDownloadClick = { patcherName, apk ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                        !context.packageManager.canRequestPackageInstalls()
                                    ) {
                                        pendingApkToDownload = Pair(patcherName, apk)
                                        showPermissionDialog = true
                                    } else {
                                        val label = "${patcherName} - ${apk.label}"
                                        viewModel.enqueueDownload(context, label, apk)
                                    }
                                }
                            )
                        }
                        Screen.README -> {
                            ReadmeWebViewScreen(rawMarkdown = state.rawMarkdown)
                        }
                        Screen.DOWNLOADS -> {
                            DownloadsScreen(
                                downloadQueue = downloadQueue,
                                onInstallClick = { task ->
                                    viewModel.installCachedFile(context, task)
                                },
                                onClearClick = {
                                    viewModel.clearQueueHistory()
                                },
                                onRetryClick = { task ->
                                    val apk = ApkInfo(label = task.label.substringAfter(" - "), url = task.apkUrl, isBeta = false, isLite = false, isOutdated = false)
                                    viewModel.enqueueDownload(context, task.label, apk)
                                }
                            )
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
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                            
                            pendingApkToDownload?.let { (patcherName, apk) ->
                                val label = "${patcherName} - ${apk.label}"
                                viewModel.enqueueDownload(context, label, apk)
                            }
                            pendingApkToDownload = null
                        }
                    }
                ) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false 
                    pendingApkToDownload = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}