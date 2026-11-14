package dev.rohitshukla.pixelscroll.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun TestScope.repo() = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { tmp.newFile("settings.preferences_pb") },
    )

    @Test
    fun `defaults match the design`() = runTest(UnconfinedTestDispatcher()) {
        val s = repo().settings.first()
        assertTrue(s.enabled)
        assertEquals(15, s.fallbackDelaySec)
    }

    @Test
    fun `writes are read back`() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo()
        repo.setEnabled(false)
        repo.setFallbackDelay(30)
        val s = repo.settings.first()
        assertFalse(s.enabled)
        assertEquals(30, s.fallbackDelaySec)
    }

    @Test
    fun `delays are kept between 3 and 120 seconds`() = runTest(UnconfinedTestDispatcher()) {
        val repo = repo()
        repo.setFallbackDelay(1)
        assertEquals(3, repo.settings.first().fallbackDelaySec)
        repo.setFallbackDelay(500)
        assertEquals(120, repo.settings.first().fallbackDelaySec)
    }
}
