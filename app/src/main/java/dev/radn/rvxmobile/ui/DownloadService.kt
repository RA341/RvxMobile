package dev.radn.rvxmobile.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import dev.radn.rvxmobile.data.DownloadQueueRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadService : Service() {
    private val client = OkHttpClient()
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val mutex = Mutex()
    private var isProcessing = false

    companion object {
        const val CHANNEL_ID = "apk_download_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1001
        const val COMPLETED_NOTIFICATION_ID_OFFSET = 2000
    }

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RvxMobile Installer")
            .setContentText("Initializing download queue...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, initialNotification)
        }

        startQueueProcessing()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for downloading ReVanced APKs"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startQueueProcessing() {
        serviceScope.launch {
            mutex.withLock {
                if (isProcessing) return@withLock
                isProcessing = true
                
                while (isActive) {
                    val nextTask = DownloadQueueRepository.getQueue().firstOrNull { it.status == DownloadStatus.QUEUED }
                    if (nextTask == null) {
                        break
                    }
                    downloadApk(nextTask)
                }
                
                isProcessing = false
                stopSelf()
            }
        }
    }

    private suspend fun downloadApk(task: DownloadTask) {
        DownloadQueueRepository.updateStatus(task.apkUrl, DownloadStatus.DOWNLOADING, 0f)
        updateForegroundNotification(task.label, 0f)

        try {
            val request = Request.Builder().url(task.apkUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("Server returned code ${response.code}")

            val body = response.body ?: throw Exception("Empty response body")
            val contentLength = body.contentLength()

            val dir = File(cacheDir, "apks")
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
                            DownloadQueueRepository.updateStatus(task.apkUrl, DownloadStatus.DOWNLOADING, progress)
                            updateForegroundNotification(task.label, progress)
                        } else {
                            DownloadQueueRepository.updateStatus(task.apkUrl, DownloadStatus.DOWNLOADING, 0.5f)
                            updateForegroundNotification(task.label, 0.5f)
                        }
                    }
                }
            }

            apkFile.setLastModified(System.currentTimeMillis())
            DownloadQueueRepository.updateStatus(task.apkUrl, DownloadStatus.COMPLETED, 1.0f)
            
            // Emit install event for foreground UI
            DownloadQueueRepository.installEvents.emit(apkFile)

            // Post completed install notification
            showCompletedNotification(task.label, apkFile)
        } catch (e: Exception) {
            DownloadQueueRepository.updateStatus(task.apkUrl, DownloadStatus.FAILED, 0f, e.localizedMessage ?: "Download failed")
            showFailedNotification(task.label, e.localizedMessage ?: "Connection error")
        }
    }

    private fun updateForegroundNotification(label: String, progress: Float) {
        val percentage = (progress * 100).toInt()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading variant")
            .setContentText("$label: $percentage%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percentage, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, builder.build())
    }

    private fun showCompletedNotification(label: String, file: File) {
        val authority = "$packageName.fileprovider"
        val uri = FileProvider.getUriForFile(this, authority, file)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(
            this,
            file.name.hashCode(),
            installIntent,
            pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Complete")
            .setContentText("Tap to install $label")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Generate unique ID based on filename hash to avoid overwriting ongoing queue notifications
        notificationManager.notify(COMPLETED_NOTIFICATION_ID_OFFSET + file.name.hashCode(), notification)
    }

    private fun showFailedNotification(label: String, errorMsg: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Download Failed")
            .setContentText("Could not download $label: $errorMsg")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(COMPLETED_NOTIFICATION_ID_OFFSET + label.hashCode(), notification)
    }
}
