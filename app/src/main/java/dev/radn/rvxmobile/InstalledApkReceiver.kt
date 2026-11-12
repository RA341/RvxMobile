package dev.radn.rvxmobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.radn.rvxmobile.data.InstalledApkCleanup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InstalledApkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED ->
                intent.data?.schemeSpecificPart ?: return
            Intent.ACTION_MY_PACKAGE_REPLACED -> context.packageName
            else -> return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                InstalledApkCleanup.clean(context.applicationContext, packageName)
            } finally {
                pending.finish()
            }
        }
    }
}
