package dev.radn.rvxmobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedUpdatesTest {
    private val pin = PinnedVariant("YouTube", "ReVanced", "arm64-v8a", false)
    private val apk = ApkInfo("arm64-v8a", "../releases/download/all/youtube.apk", false, false, false)
    private val url = resolvePinnedUrl(apk.url)
    private val asset = ReleaseAsset("youtube.apk", 100L, 0, url, "2026-01-02T00:00:00Z")
    private val installed = mapOf(url to 1767225600000L) // 2026-01-01 UTC

    private fun apps(apks: List<ApkInfo> = listOf(apk)) = listOf(
        AppInfo("youtube", "", listOf(PatcherInfo("revanced", "", apks)))
    )

    @Test
    fun matchesPinnedVariantToNewerLiveAsset() {
        val updates = findPinnedUpdates(listOf(pin), apps(), listOf(asset), installed)
        assertEquals(listOf(PinnedUpdate(pin, asset)), updates)
    }

    @Test
    fun prefersMatchingBetaChannelWhenLabelsAreTheSame() {
        val beta = apk.copy(url = "https://example.com/beta.apk", isBeta = true)
        val betaAsset = asset.copy(browserDownloadUrl = beta.url)
        val updates = findPinnedUpdates(listOf(pin.copy(isBeta = true)), apps(listOf(apk, beta)),
            listOf(asset, betaAsset), installed + (beta.url to installed.getValue(url)))
        assertEquals(beta.url, updates.single().asset.browserDownloadUrl)
    }

    @Test
    fun removedPinsAndMissingAssetsDoNotProduceUpdates() {
        assertTrue(findPinnedUpdates(emptyList(), apps(), listOf(asset), installed).isEmpty())
        assertTrue(findPinnedUpdates(listOf(pin), apps(emptyList()), listOf(asset), installed).isEmpty())
        assertTrue(findPinnedUpdates(listOf(pin), apps(), emptyList(), installed).isEmpty())
    }

    @Test
    fun noAlertBeforeFirstInstallOrAfterInstallingLatestRelease() {
        assertTrue(findPinnedUpdates(listOf(pin), apps(), listOf(asset), emptyMap()).isEmpty())
        assertTrue(findPinnedUpdates(listOf(pin), apps(), listOf(asset),
            mapOf(url to 1767398400000L)).isEmpty())
    }

    @Test
    fun fingerprintIsStableUntilReleaseChanges() {
        val update = PinnedUpdate(pin, asset)
        assertEquals(update.fingerprint, PinnedUpdate(pin, asset.copy(downloadCount = 10)).fingerprint)
        assertNotEquals(update.fingerprint,
            PinnedUpdate(pin, asset.copy(updatedAt = "2026-01-03T00:00:00Z")).fingerprint)
    }
}
