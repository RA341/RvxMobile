package dev.radn.rvxmobile.data

internal data class PinnedUpdate(val pin: PinnedVariant, val asset: ReleaseAsset) {
    val fingerprint: String
        get() = "${pin.toSerializedString()}|${asset.browserDownloadUrl}|${asset.updatedAt}"
}

internal fun resolvePinnedUrl(url: String): String = if (url.startsWith("http")) url else {
    val path = url.substringAfter("releases/download/")
    "https://github.com/FiorenMas/Revanced-And-Revanced-Extended-Non-Root/releases/download/$path"
}

internal fun findPinnedUpdates(
    pins: List<PinnedVariant>,
    apps: List<AppInfo>,
    assets: List<ReleaseAsset>,
    lastInstalled: Map<String, Long>
): List<PinnedUpdate> = pins.mapNotNull { pin ->
    val app = apps.find { it.name.equals(pin.appName, ignoreCase = true) }
    val patcher = app?.patchers?.find { it.name.equals(pin.patcherName, ignoreCase = true) }
    val apk = patcher?.apks?.find {
        it.label.trim() == pin.apkLabel.trim() && it.isBeta == pin.isBeta
    } ?: patcher?.apks?.find { it.label.trim() == pin.apkLabel.trim() }
    val url = apk?.let { resolvePinnedUrl(it.url) } ?: return@mapNotNull null
    val asset = assets.find { it.browserDownloadUrl == url }
    if (asset != null && hasPinnedUpdate(asset.updatedAt, lastInstalled[url] ?: 0L)) {
        PinnedUpdate(pin, asset)
    } else null
}
