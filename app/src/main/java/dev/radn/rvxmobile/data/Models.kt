package dev.radn.rvxmobile.data

data class AppInfo(
    val name: String,
    val playStoreLink: String,
    val patchers: List<PatcherInfo>
)

data class PatcherInfo(
    val name: String,
    val githubLink: String,
    val apks: List<ApkInfo>
)

data class ApkInfo(
    val label: String,
    val url: String,
    val isBeta: Boolean,
    val isLite: Boolean,
    val isOutdated: Boolean
)
