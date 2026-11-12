package dev.radn.rvxmobile.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.radn.rvxmobile.updates.PinnedUpdateScheduler
import dev.radn.rvxmobile.updates.PinnedUpdateWorker
import dev.radn.rvxmobile.updates.UpdateFrequency

@Composable
internal fun PinnedUpdateSettings() {
    val context = LocalContext.current
    var frequency by remember { mutableStateOf(PinnedUpdateScheduler.frequency(context)) }
    var expanded by remember { mutableStateOf(false) }
    var notificationsEnabled by remember {
        mutableStateOf(PinnedUpdateWorker.notificationsEnabled(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = PinnedUpdateWorker.notificationsEnabled(context)
        if (granted) PinnedUpdateScheduler.schedule(context)
    }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        notificationsEnabled = PinnedUpdateWorker.notificationsEnabled(context)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Pinned app updates", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Check for new releases of pinned apps you have installed through RvxMobile.",
                style = MaterialTheme.typography.bodySmall)
            Box {
                Button(onClick = { expanded = true }) {
                    Text("Check frequency: ${frequency.label}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    UpdateFrequency.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                frequency = option
                                expanded = false
                                PinnedUpdateScheduler.setFrequency(context, option)
                            }
                        )
                    }
                }
            }
            if (frequency != UpdateFrequency.OFF) {
                Text("Checks run in the background when connected. Android may delay them to save battery. Tap an update notification to open Pinned.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(if (notificationsEnabled) "Notifications enabled" else "Notifications disabled",
                    style = MaterialTheme.typography.bodySmall)
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    TextButton(onClick = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text("Enable update notifications") }
                }
                TextButton(onClick = {
                    settingsLauncher.launch(notificationSettingsIntent(context))
                }) { Text("Notification settings") }
            }
        }
    }
}

private fun notificationSettingsIntent(context: Context): Intent = if (Build.VERSION.SDK_INT >= 26) {
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
} else {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
