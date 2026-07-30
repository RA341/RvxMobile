package dev.radn.rvxmobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Installer", Icons.AutoMirrored.Filled.List),
    README("README", Icons.Default.Info),
    DOWNLOADS("Downloads", Icons.Default.Download),
    SETTINGS("Settings", Icons.Default.Settings)
}
