package dev.radn.rvxmobile.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledApkCleanupTest {
    @Test fun successfulInstallRemovesMatchingDownload() {
        assertTrue(wasApkInstalled(2, 2, 100, 200, 150))
    }

    @Test fun cancelledUpdateKeepsDownload() {
        assertFalse(wasApkInstalled(2, 1, 100, 100, 150))
    }

    @Test fun alreadyInstalledVersionDoesNotCountAsSuccess() {
        assertFalse(wasApkInstalled(2, 2, 100, 100, 150))
    }

    @Test fun successfulReinstallOfSameVersionCountsAsSuccess() {
        assertTrue(wasApkInstalled(2, 2, 100, 200, 150))
    }

    @Test fun installingAnotherVersionKeepsDownload() {
        assertFalse(wasApkInstalled(2, 3, 100, 200, 150))
    }

    @Test fun previouslyHiddenInstallationDoesNotCountAsSuccess() {
        assertFalse(wasApkInstalled(2, 2, 0, 100, 150))
    }

    @Test fun firstInstallationCountsAsSuccess() {
        assertTrue(wasApkInstalled(2, 2, 0, 200, 150))
    }
}
