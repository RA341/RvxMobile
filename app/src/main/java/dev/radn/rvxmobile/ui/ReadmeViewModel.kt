package dev.radn.rvxmobile.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.radn.rvxmobile.data.AppInfo
import dev.radn.rvxmobile.data.ApkInfo
import dev.radn.rvxmobile.data.ReadmeParser
import dev.radn.rvxmobile.data.DownloadQueueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed interface UiState {
    object Loading : UiState
    data class Success(val apps: List<AppInfo>, val rawMarkdown: String) : UiState
    data class Error(val message: String) : UiState
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class DownloadTask(
    val apkUrl: String,
    val label: String,
    val progress: Float,
    val status: DownloadStatus,
    val errorMsg: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class ReadmeViewModel : ViewModel() {
    private val client = OkHttpClient()
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    // Expose download queue flow from the centralized repository
    val downloadQueue: StateFlow<List<DownloadTask>> = DownloadQueueRepository.downloadQueue

    // Expose install prompt flow from the centralized repository
    val installEvent: SharedFlow<File> = DownloadQueueRepository.installEvents

    val deviceAbi: String by lazy {
        Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }

    init {
        loadData()
    }

    fun loadData() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val (rawMarkdown, apps) = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://raw.githubusercontent.com/FiorenMas/Revanced-And-Revanced-Extended-Non-Root/main/README.md")
                        .build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw Exception("Failed to fetch README: ${response.code}")
                    val md = response.body?.string() ?: ""
                    val parsed = ReadmeParser.parse(md)
                    Pair(md, parsed)
                }
                _uiState.value = UiState.Success(apps, rawMarkdown)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun resolveUrl(url: String): String {
        return if (url.startsWith("http")) {
            url
        } else {
            val cleanPath = url.substringAfter("releases/download/")
            "https://github.com/FiorenMas/Revanced-And-Revanced-Extended-Non-Root/releases/download/$cleanPath"
        }
    }

    fun enqueueDownload(context: Context, label: String, apk: ApkInfo) {
        val absoluteUrl = resolveUrl(apk.url)
        val filename = absoluteUrl.substringAfterLast("/")
        val cacheDir = File(context.cacheDir, "apks")
        val apkFile = File(cacheDir, filename)
        
        val fiveMinutesMs = 5 * 60 * 1000L
        val isFresh = apkFile.exists() && (System.currentTimeMillis() - apkFile.lastModified() < fiveMinutesMs)

        if (isFresh) {
            // Fresh cache exists, skip queueing/downloading
            viewModelScope.launch {
                val completedTask = DownloadTask(
                    apkUrl = absoluteUrl,
                    label = label,
                    progress = 1.0f,
                    status = DownloadStatus.COMPLETED
                )
                DownloadQueueRepository.addTask(completedTask)
                DownloadQueueRepository.updateStatus(absoluteUrl, DownloadStatus.COMPLETED, 1.0f)
                Toast.makeText(context, "Using fresh cached copy", Toast.LENGTH_SHORT).show()
                DownloadQueueRepository.installEvents.emit(apkFile)
            }
            return
        }

        // Add task to repository queue
        val task = DownloadTask(
            apkUrl = absoluteUrl,
            label = label,
            progress = 0f,
            status = DownloadStatus.QUEUED
        )
        DownloadQueueRepository.addTask(task)

        // Start Foreground Download Service
        val serviceIntent = Intent(context, DownloadService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    fun installCachedFile(context: Context, task: DownloadTask) {
        val filename = task.apkUrl.substringAfterLast("/")
        val cacheDir = File(context.cacheDir, "apks")
        val file = File(cacheDir, filename)
        if (file.exists()) {
            installApk(context, file)
        } else {
            // Re-download
            enqueueDownload(context, task.label, ApkInfo(label = task.label.substringAfter(" - "), url = task.apkUrl, isBeta = false, isLite = false, isOutdated = false))
        }
    }

    fun installApk(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun clearQueueHistory() {
        DownloadQueueRepository.clearHistory()
    }
}
