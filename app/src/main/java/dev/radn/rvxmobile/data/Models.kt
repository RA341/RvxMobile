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

data class PinnedVariant(
    val appName: String,
    val patcherName: String,
    val apkLabel: String,
    val isBeta: Boolean
) {
    fun toSerializedString(): String = "$appName|$patcherName|$apkLabel|$isBeta"
    
    companion object {
        fun fromSerializedString(str: String): PinnedVariant? {
            val parts = str.split("|")
            if (parts.size < 4) return null
            return PinnedVariant(
                appName = parts[0],
                patcherName = parts[1],
                apkLabel = parts[2],
                isBeta = parts[3].toBoolean()
            )
        }
    }
}

