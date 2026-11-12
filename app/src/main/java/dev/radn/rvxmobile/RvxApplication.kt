package dev.radn.rvxmobile

import android.app.Application
import dev.radn.rvxmobile.updates.PinnedUpdateScheduler

class RvxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PinnedUpdateScheduler.schedule(this)
    }
}
