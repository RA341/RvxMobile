package dev.radn.rvxmobile.updates

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import dev.radn.rvxmobile.MainActivity
import dev.radn.rvxmobile.data.PinnedVariant
import dev.radn.rvxmobile.data.ReadmeParser
import dev.radn.rvxmobile.data.ReleaseAsset
import dev.radn.rvxmobile.data.findPinnedUpdates
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class PinnedUpdateWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val context = applicationContext
        val prefs = context.getSharedPreferences(PinnedUpdateScheduler.PREFS, Context.MODE_PRIVATE)
        fun pins() = prefs.getStringSet("pinned_variants", emptySet()).orEmpty()
            .mapNotNull { PinnedVariant.fromSerializedString(it) }
        if (PinnedUpdateScheduler.frequency(context) == UpdateFrequency.OFF || pins().isEmpty()) {
            clearNotification(context)
            return Result.success()
        }
        val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).build()
        fun fetch(url: String): String {
            val request = Request.Builder().url(url).header("User-Agent", "RvxMobile").build()
            return client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Update check failed: ${response.code}")
                response.body?.string() ?: throw IOException("Empty update response")
            }
        }
        return try {
            val apps = ReadmeParser.parse(fetch(
                "https://raw.githubusercontent.com/FiorenMas/Revanced-And-Revanced-Extended-Non-Root/main/README.md"
            ))
            val json = JSONObject(fetch(
                "https://api.github.com/repos/FiorenMas/Revanced-And-Revanced-Extended-Non-Root/releases/tags/all"
            )).getJSONArray("assets")
            val assets = (0 until json.length()).map { index ->
                val asset = json.getJSONObject(index)
                ReleaseAsset(asset.getString("name"), asset.getLong("size"),
                    asset.getInt("download_count"), asset.getString("browser_download_url"),
                    asset.getString("updated_at"))
            }
            // Re-read user choices after the network calls so unpinned/disabled items do not alert.
            if (isStopped || PinnedUpdateScheduler.frequency(context) == UpdateFrequency.OFF) {
                return Result.success()
            }
            val installed = prefs.all.filterKeys { it.startsWith("last_installed_") }
                .mapKeys { it.key.removePrefix("last_installed_") }
                .mapValues { (it.value as? Long) ?: 0L }
            val updates = findPinnedUpdates(pins(), apps, assets, installed)
            val fingerprints = updates.map { it.fingerprint }.toSet()
            val notified = prefs.getStringSet("notified_pinned_updates", emptySet()).orEmpty()
            if (updates.isEmpty()) clearNotification(context)
            if (updates.any { it.fingerprint !in notified }) {
                val names = updates.map { "${it.pin.appName} (${it.pin.patcherName})" }.distinct()
                if (!notifyUpdates(context, names)) return Result.success()
            }
            prefs.edit().putStringSet("notified_pinned_updates", fingerprints).apply()
            Result.success()
        } catch (e: IOException) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } catch (e: org.json.JSONException) {
            Result.failure()
        }
    }

    private fun notifyUpdates(context: Context, names: List<String>): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= 26) {
            val system = context.getSystemService(NotificationManager::class.java)
            system.createNotificationChannel(NotificationChannel(
                CHANNEL, "Pinned app updates", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "New releases for your pinned apps" })
        }
        if (!notificationsEnabled(context)) return false
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(OPEN_PINNED, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = names.joinToString(", ")
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Pinned app updates available")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\nOpen Pinned to update."))
            .setContentIntent(pending)
            .addAction(0, "View updates", pending)
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    companion object {
        const val OPEN_PINNED = "open_pinned_updates"
        private const val CHANNEL = "pinned_app_updates"
        private const val NOTIFICATION_ID = 41001

        fun notificationsEnabled(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) return false
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
        }

        fun clearNotification(context: Context) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}
