package dev.radn.rvxmobile.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.radn.rvxmobile.data.ApkInfo
import dev.radn.rvxmobile.data.AppInfo
import dev.radn.rvxmobile.data.DownloadQueueRepository
import dev.radn.rvxmobile.data.PinnedVariant
import dev.radn.rvxmobile.data.ReadmeParser
import dev.radn.rvxmobile.data.PinnedReleaseAsset
import dev.radn.rvxmobile.data.ReleaseAsset
import dev.radn.rvxmobile.data.ReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.Locale

sealed interface UiState {
    object Loading : UiState
    data class Success(val apps: List<AppInfo>, val rawMarkdown: String) : UiState
    data class Error(val message: String) : UiState
}

sealed interface ReleasesState {
    object Loading : ReleasesState
    data class Success(val release: ReleaseInfo) : ReleasesState
    data class Error(val message: String) : ReleasesState
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

class ReadmeViewModel(application: Application) : AndroidViewModel(application) {
    private val client = OkHttpClient()
    
    private val prefs = application.getSharedPreferences("rvx_mobile_pins", Context.MODE_PRIVATE)
    
    private val _pinnedVariants = MutableStateFlow<List<PinnedVariant>>(emptyList())
    val pinnedVariants: StateFlow<List<PinnedVariant>> = _pinnedVariants

    private val _pinnedReleases = MutableStateFlow<List<PinnedReleaseAsset>>(emptyList())
    val pinnedReleases: StateFlow<List<PinnedReleaseAsset>> = _pinnedReleases

    private val _lastInstalled = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastInstalled: StateFlow<Map<String, Long>> = _lastInstalled

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val _releasesState = MutableStateFlow<ReleasesState>(ReleasesState.Loading)
    val releasesState: StateFlow<ReleasesState> = _releasesState

    // Expose download queue flow from the centralized repository
    val downloadQueue: StateFlow<List<DownloadTask>> = DownloadQueueRepository.downloadQueue

    // Expose install prompt flow from the centralized repository
    val installEvent: SharedFlow<File> = DownloadQueueRepository.installEvents

    val deviceAbi: String by lazy {
        Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }

    init {
        loadPins()
        loadData()
        loadReleases()
        syncQueueWithCache()
    }

    private fun loadPins() {
        val saved = prefs.getStringSet("pinned_variants", emptySet()) ?: emptySet()
        _pinnedVariants.value = saved.mapNotNull { PinnedVariant.fromSerializedString(it) }

        val savedReleases = prefs.getStringSet("pinned_releases", emptySet()) ?: emptySet()
        _pinnedReleases.value = savedReleases.mapNotNull { PinnedReleaseAsset.fromSerializedString(it) }

        val all = prefs.all
        val stamps = all.filterKeys { it.startsWith("last_installed_") }
            .mapKeys { it.key.substringAfter("last_installed_") }
            .mapValues { (it.value as? Long) ?: 0L }
        _lastInstalled.value = stamps
    }

    fun recordInstallation(apkUrl: String) {
        val now = System.currentTimeMillis()
        prefs.edit().putLong("last_installed_$apkUrl", now).apply()
        
        val current = _lastInstalled.value.toMutableMap()
        current[apkUrl] = now
        _lastInstalled.value = current
    }

    fun togglePin(appName: String, patcherName: String, apk: ApkInfo) {
        val current = _pinnedVariants.value.toMutableList()
        val existing = current.find { 
            it.appName == appName && it.patcherName == patcherName && it.apkLabel == apk.label && it.isBeta == apk.isBeta 
        }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(PinnedVariant(
                appName = appName,
                patcherName = patcherName,
                apkLabel = apk.label,
                isBeta = apk.isBeta
            ))
        }
        _pinnedVariants.value = current
        prefs.edit().putStringSet("pinned_variants", current.map { it.toSerializedString() }.toSet()).apply()
    }

    fun isPinned(appName: String, patcherName: String, apk: ApkInfo): Boolean {
        return _pinnedVariants.value.any { 
            it.appName == appName && it.patcherName == patcherName && it.apkLabel == apk.label && it.isBeta == apk.isBeta 
        }
    }

    fun togglePinRelease(asset: ReleaseAsset) {
        val current = _pinnedReleases.value.toMutableList()
        val existing = current.find { it.browserDownloadUrl == asset.browserDownloadUrl }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(PinnedReleaseAsset(
                name = asset.name,
                size = asset.size,
                downloadCount = asset.downloadCount,
                browserDownloadUrl = asset.browserDownloadUrl,
                updatedAt = asset.updatedAt
            ))
        }
        _pinnedReleases.value = current
        prefs.edit().putStringSet("pinned_releases", current.map { it.toSerializedString() }.toSet()).apply()
    }

    fun togglePinReleaseAsset(pinnedAsset: PinnedReleaseAsset) {
        val current = _pinnedReleases.value.toMutableList()
        val existing = current.find { it.browserDownloadUrl == pinnedAsset.browserDownloadUrl }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add(pinnedAsset)
        }
        _pinnedReleases.value = current
        prefs.edit().putStringSet("pinned_releases", current.map { it.toSerializedString() }.toSet()).apply()
    }

    fun isReleasePinned(asset: ReleaseAsset): Boolean {
        return _pinnedReleases.value.any { it.browserDownloadUrl == asset.browserDownloadUrl }
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
            recordInstallation(task.apkUrl)
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
        val cacheDir = File(getApplication<Application>().cacheDir, "apks")
        val currentQueue = DownloadQueueRepository.getQueue()
        val tasksToRemove = currentQueue.filterNot {
            it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
        }
        tasksToRemove.forEach { task ->
            val filename = task.apkUrl.substringAfterLast("/")
            val file = File(cacheDir, filename)
            if (file.exists()) {
                file.delete()
            }
        }
        DownloadQueueRepository.clearHistory()
    }

    fun removeDownloadTask(task: DownloadTask) {
        val cacheDir = File(getApplication<Application>().cacheDir, "apks")
        val filename = task.apkUrl.substringAfterLast("/")
        val file = File(cacheDir, filename)
        if (file.exists()) {
            file.delete()
        }
        DownloadQueueRepository.removeTask(task.apkUrl)
    }

    fun syncQueueWithCache() {
        val cacheDir = File(getApplication<Application>().cacheDir, "apks")
        DownloadQueueRepository.syncQueueWithCache(cacheDir)
    }

    // Get total size of APK cache files
    fun getCacheSize(context: Context): String {
        syncQueueWithCache()
        val cacheDir = File(context.cacheDir, "apks")
        if (!cacheDir.exists() || !cacheDir.isDirectory) return "0 B"
        val bytes = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.2f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format(Locale.getDefault(), "%.2f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }

    // Clear APK cache files
    fun clearCache(context: Context) {
        val cacheDir = File(context.cacheDir, "apks")
        if (cacheDir.exists() && cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
        syncQueueWithCache()
    }

    fun loadReleases() {
        _releasesState.value = ReleasesState.Loading
        viewModelScope.launch {
            try {
                val releaseInfo = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/FiorenMas/Revanced-And-Revanced-Extended-Non-Root/releases/tags/all")
                        .header("User-Agent", "RvxMobile")
                        .build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) throw Exception("Failed to fetch releases: ${response.code}")
                    val jsonStr = response.body?.string() ?: throw Exception("Empty body")
                    parseReleaseInfo(jsonStr)
                }
                _releasesState.value = ReleasesState.Success(releaseInfo)
            } catch (e: Exception) {
                _releasesState.value = ReleasesState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    private fun parseReleaseInfo(jsonStr: String): ReleaseInfo {
        val obj = org.json.JSONObject(jsonStr)
        val assetsArray = obj.getJSONArray("assets")
        val assets = mutableListOf<ReleaseAsset>()
        for (i in 0 until assetsArray.length()) {
            val assetObj = assetsArray.getJSONObject(i)
            assets.add(
                ReleaseAsset(
                    name = assetObj.getString("name"),
                    size = assetObj.getLong("size"),
                    downloadCount = assetObj.getInt("download_count"),
                    browserDownloadUrl = assetObj.getString("browser_download_url"),
                    updatedAt = assetObj.getString("updated_at")
                )
            )
        }
        return ReleaseInfo(
            id = obj.getLong("id"),
            tagName = obj.getString("tag_name"),
            name = obj.getString("name"),
            publishedAt = obj.getString("published_at"),
            htmlUrl = obj.getString("html_url"),
            assets = assets
        )
    }
}
