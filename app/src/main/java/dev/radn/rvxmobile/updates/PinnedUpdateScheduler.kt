package dev.radn.rvxmobile.updates

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

enum class UpdateFrequency(val hours: Long, val label: String) {
    OFF(0, "Off"),
    HOURLY(1, "Every hour"),
    SIX_HOURS(6, "Every 6 hours"),
    DAILY(24, "Once a day"),
    WEEKLY(168, "Once a week");

    companion object {
        fun fromHours(hours: Long) = entries.find { it.hours == hours } ?: DAILY
    }
}

object PinnedUpdateScheduler {
    const val PREFS = "rvx_mobile_pins"
    private const val FREQUENCY = "pinned_update_frequency_hours"
    private const val WORK_NAME = "pinned-app-update-check"

    fun frequency(context: Context): UpdateFrequency = UpdateFrequency.fromHours(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(FREQUENCY, 24L)
    )

    fun setFrequency(context: Context, frequency: UpdateFrequency) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(FREQUENCY, frequency.hours).apply()
        schedule(context)
    }

    fun schedule(context: Context) {
        val manager = WorkManager.getInstance(context)
        val frequency = frequency(context)
        if (frequency == UpdateFrequency.OFF) {
            manager.cancelUniqueWork(WORK_NAME)
            PinnedUpdateWorker.clearNotification(context)
            return
        }
        val request = PeriodicWorkRequestBuilder<PinnedUpdateWorker>(frequency.hours, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        // UPDATE preserves the existing schedule across application launches.
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
