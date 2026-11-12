package dev.radn.rvxmobile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import dev.radn.rvxmobile.data.InstalledApkCleanup
import java.io.File

/** Receives the system installer's result for both UI and notification installs. */
class InstallApkActivity : ComponentActivity() {
    companion object {
        private const val FILENAME = "apk_filename"
        private const val MODIFIED = "apk_modified"
        private const val SIZE = "apk_size"

        fun createIntent(context: Context, file: File): Intent =
            Intent(context, InstallApkActivity::class.java).apply {
                putExtra(FILENAME, file.name)
                putExtra(MODIFIED, file.lastModified())
                putExtra(SIZE, file.length())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }

    private val installer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            apkFile()?.let { file ->
                InstalledApkCleanup.deleteInstalled(this, file,
                    intent.getLongExtra(MODIFIED, -1), intent.getLongExtra(SIZE, -1))
            }
        }
        finish()
    }

    @Suppress("DEPRECATION") // ACTION_INSTALL_PACKAGE supplies a result for the existing APK intent flow.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The activity result registry restores the outstanding request after recreation.
        if (savedInstanceState != null) return
        val file = apkFile()
        if (file == null || !file.exists()) {
            Toast.makeText(this, "APK is no longer cached. Download it again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        try {
            InstalledApkCleanup.track(this, file)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            installer.launch(Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            })
        } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "No APK installer available", Toast.LENGTH_SHORT).show()
            finish()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Allow RvxMobile to install unknown apps and try again", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun apkFile(): File? {
        val filename = intent.getStringExtra(FILENAME) ?: return null
        if (filename != File(filename).name) return null
        return File(File(cacheDir, "apks"), filename)
    }
}
