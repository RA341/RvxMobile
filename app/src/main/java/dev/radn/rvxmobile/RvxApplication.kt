package dev.radn.rvxmobile

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dev.radn.rvxmobile.data.InstalledApkCleanup
import dev.radn.rvxmobile.updates.PinnedUpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RvxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Keep observing while the installer covers the activity or the UI is closed.
        ContextCompat.registerReceiver(this, InstalledApkReceiver(), IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }, ContextCompat.RECEIVER_EXPORTED)
        CoroutineScope(Dispatchers.IO).launch {
            InstalledApkCleanup.clean(this@RvxApplication)
        }
        PinnedUpdateScheduler.schedule(this)
    }
}
