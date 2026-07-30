package dev.radn.rvxmobile.data

import dev.radn.rvxmobile.ui.DownloadStatus
import dev.radn.rvxmobile.ui.DownloadTask
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

object DownloadQueueRepository {
    private val _downloadQueue = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadQueue: StateFlow<List<DownloadTask>> = _downloadQueue

    // Shared flow to publish install prompts to the active UI viewmodel
    val installEvents = MutableSharedFlow<File>()

    fun getQueue(): List<DownloadTask> = _downloadQueue.value

    fun addTask(task: DownloadTask) {
        val exists = _downloadQueue.value.any { it.apkUrl == task.apkUrl }
        if (!exists) {
            _downloadQueue.value = _downloadQueue.value + task
        } else {
            updateStatus(task.apkUrl, DownloadStatus.QUEUED, 0f)
        }
    }

    fun updateStatus(apkUrl: String, status: DownloadStatus, progress: Float = 0f, errorMsg: String? = null) {
        _downloadQueue.value = _downloadQueue.value.map {
            if (it.apkUrl == apkUrl) {
                it.copy(status = status, progress = progress, errorMsg = errorMsg, timestamp = System.currentTimeMillis())
            } else {
                it
            }
        }
    }

    fun clearHistory() {
        _downloadQueue.value = _downloadQueue.value.filter {
            it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING
        }
    }
}
