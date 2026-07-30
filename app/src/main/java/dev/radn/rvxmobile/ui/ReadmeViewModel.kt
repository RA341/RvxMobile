package dev.radn.rvxmobile.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.radn.rvxmobile.data.AppInfo
import dev.radn.rvxmobile.data.ApkInfo
import dev.radn.rvxmobile.data.ReadmeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

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

    // Download Queue State Flow
    private val _downloadQueue = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadQueue: StateFlow<List<DownloadTask>> = _downloadQueue

    // Install prompt Shared Flow (emits File when ready for install)
    private val _installEvent = MutableSharedFlow<File>()
    val installEvent: SharedFlow<File> = _installEvent

    private val queueMutex = Mutex()
    private var isProcessingQueue = false

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
            // Fresh cached version exists, skip downloading
            viewModelScope.launch {
                val existingTask = _downloadQueue.value.firstOrNull { it.apkUrl == absoluteUrl }
                if (existingTask == null) {
                    val completedTask = DownloadTask(
                        apkUrl = absoluteUrl,
                        label = label,
                        progress = 1.0f,
                        status = DownloadStatus.COMPLETED
                    )
                    _downloadQueue.value = _downloadQueue.value + completedTask
                } else {
                    updateTaskStatus(absoluteUrl, DownloadStatus.COMPLETED, 1.0f)
                }
                Toast.makeText(context, "Using fresh cached copy", Toast.LENGTH_SHORT).show()
                _installEvent.emit(apkFile)
            }
            return
        }

        // Add or update task in queue
        val existingTask = _downloadQueue.value.firstOrNull { it.apkUrl == absoluteUrl }
        if (existingTask == null) {
            val newTask = DownloadTask(
                apkUrl = absoluteUrl,
                label = label,
                progress = 0f,
                status = DownloadStatus.QUEUED
            )
            _downloadQueue.value = _downloadQueue.value + newTask
            Toast.makeText(context, "Added to download queue", Toast.LENGTH_SHORT).show()
        } else if (existingTask.status == DownloadStatus.FAILED || existingTask.status == DownloadStatus.COMPLETED) {
            updateTaskStatus(absoluteUrl, DownloadStatus.QUEUED, 0f)
            Toast.makeText(context, "Retrying download...", Toast.LENGTH_SHORT).show()
        }

        startQueueProcessing(context.applicationContext)
    }

    private fun startQueueProcessing(context: Context) {
        viewModelScope.launch {
            queueMutex.withLock {
                if (isProcessingQueue) return@withLock
                isProcessingQueue = true
                
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        while (true) {
                            val nextTask = _downloadQueue.value.firstOrNull { it.status == DownloadStatus.QUEUED }
                            if (nextTask == null) {
                                break
                            }
                            processDownloadTask(context, nextTask)
                        }
                    } finally {
                        isProcessingQueue = false
                    }
                }
            }
        }
    }

    private suspend fun processDownloadTask(context: Context, task: DownloadTask) {
        updateTaskStatus(task.apkUrl, DownloadStatus.DOWNLOADING, 0f)
        
        try {
            val request = Request.Builder().url(task.apkUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Server returned code ${response.code}")
            
            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength()
            
            val dir = File(context.cacheDir, "apks")
            if (!dir.exists()) dir.mkdirs()
            
            val apkFile = File(dir, task.apkUrl.substringAfterLast("/"))
            
            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val progress = totalBytesRead.toFloat() / contentLength
                            updateTaskStatus(task.apkUrl, DownloadStatus.DOWNLOADING, progress)
                        } else {
                            updateTaskStatus(task.apkUrl, DownloadStatus.DOWNLOADING, 0.5f)
                        }
                    }
                }
            }
            
            // Set timestamp to now, indicating cache freshness
            apkFile.setLastModified(System.currentTimeMillis())

            updateTaskStatus(task.apkUrl, DownloadStatus.COMPLETED, 1.0f)
            _installEvent.emit(apkFile)
        } catch (e: Exception) {
            updateTaskStatus(task.apkUrl, DownloadStatus.FAILED, 0f, e.localizedMessage ?: "Download failed")
        }
    }

    private fun updateTaskStatus(apkUrl: String, status: DownloadStatus, progress: Float = 0f, errorMsg: String? = null) {
        _downloadQueue.value = _downloadQueue.value.map {
            if (it.apkUrl == apkUrl) {
                it.copy(status = status, progress = progress, errorMsg = errorMsg, timestamp = System.currentTimeMillis())
            } else {
                it
            }
        }
    }

    fun installCachedFile(context: Context, task: DownloadTask) {
        val filename = task.apkUrl.substringAfterLast("/")
        val cacheDir = File(context.cacheDir, "apks")
        val file = File(cacheDir, filename)
        if (file.exists()) {
            installApk(context, file)
        } else {
            // Redownload
            updateTaskStatus(task.apkUrl, DownloadStatus.QUEUED, 0f)
            startQueueProcessing(context.applicationContext)
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
        // Clear tasks that are either completed or failed
        _downloadQueue.value = _downloadQueue.value.filter {
            it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
        }
    }
}
