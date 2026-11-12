package dev.radn.rvxmobile.data

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import dev.radn.rvxmobile.ui.DownloadService
import dev.radn.rvxmobile.ui.DownloadStatus
import org.json.JSONObject
import java.io.File

internal fun wasApkInstalled(
    downloadedVersion: Long,
    installedVersion: Long,
    previousUpdateTime: Long,
    installedUpdateTime: Long,
    trackedAt: Long
): Boolean = downloadedVersion == installedVersion &&
    installedUpdateTime > previousUpdateTime && installedUpdateTime >= trackedAt

/** Only completed downloads with a subsequent successful installation are removed. */
object InstalledApkCleanup {
    private const val PREFS = "pending_apk_cleanup"

    @Synchronized
    fun track(context: Context, file: File) {
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(file.name, null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        if (previous != null && previous.optLong("modified") == file.lastModified() &&
            previous.optLong("size") == file.length()) return

        val record = JSONObject().apply {
            put("trackedAt", System.currentTimeMillis())
            put("package", archive.packageName)
            put("version", versionCode(archive))
            put("previousUpdate", installedPackage(context, archive.packageName)?.lastUpdateTime ?: 0L)
            put("modified", file.lastModified())
            put("size", file.length())
        }
        // Persist before opening the installer, including when installing our own update.
        prefs.edit().putString(file.name, record.toString()).commit()
    }

    @Synchronized
    fun clean(context: Context, packageName: String? = null) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cacheDir = File(context.cacheDir, "apks")
        for ((filename, value) in prefs.all) {
            val record = runCatching { JSONObject(value as String) }.getOrNull() ?: continue
            if (packageName != null && record.getString("package") != packageName) continue
            val file = File(cacheDir, filename)
            if (!file.exists()) {
                prefs.edit().remove(filename).apply()
                continue
            }
            // A new download may be writing to the same filename. Never remove it.
            val active = DownloadQueueRepository.getQueue().any {
                it.apkUrl.substringAfterLast("/") == filename &&
                    (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED)
            }
            if (active || file.lastModified() != record.getLong("modified") ||
                file.length() != record.getLong("size")) continue
            val installed = installedPackage(context, record.getString("package")) ?: continue
            if (!wasApkInstalled(record.getLong("version"), versionCode(installed),
                    record.getLong("previousUpdate"), installed.lastUpdateTime, record.getLong("trackedAt"))) continue

            deleteInstalled(context, file, record.getLong("modified"), record.getLong("size"))
        }
    }

    @Synchronized
    fun deleteInstalled(context: Context, file: File, modified: Long, size: Long) {
        if (file.lastModified() != modified || file.length() != size) return
        if (DownloadQueueRepository.getQueue().any {
                it.apkUrl.substringAfterLast("/") == file.name &&
                    (it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED)
            }) return
        if (file.delete()) {
            val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifications.cancel(DownloadService.COMPLETED_NOTIFICATION_ID_OFFSET + file.name.hashCode())
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(file.name).apply()
            DownloadQueueRepository.syncQueueWithCache(file.parentFile!!)
        }
    }

    private fun installedPackage(context: Context, packageName: String): PackageInfo? = try {
        context.packageManager.getPackageInfo(packageName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
}
