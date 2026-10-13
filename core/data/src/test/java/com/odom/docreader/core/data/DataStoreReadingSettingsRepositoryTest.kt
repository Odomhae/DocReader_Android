package com.odom.docreader.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.odom.docreader.core.model.PaperColor
import com.odom.docreader.core.model.ReadingSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreReadingSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.createRepository(): DataStoreReadingSettingsRepository {
        val file = temporaryFolder.root.resolve("settings.preferences_pb")
        return DataStoreReadingSettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
    }

    @Test
    fun defaultsBeforeAnythingIsSaved() = runTest {
        val repository = createRepository()

        assertEquals(ReadingSettings(), repository.readingSettings.first())
    }

    @Test
    fun paperColorIsReadBack() = runTest {
        val repository = createRepository()

        repository.setPaperColor(PaperColor.SEPIA)

        assertEquals(PaperColor.SEPIA, repository.readingSettings.first().paperColor)
    }

    @Test
    fun settingBrightnessStopsFollowingTheSystem() = runTest {
        val repository = createRepository()

        repository.setBrightness(0.2f)

        assertEquals(
            ReadingSettings(useSystemBrightness = false, brightness = 0.2f),
            repository.readingSettings.first(),
        )
    }

    @Test
    fun followingTheSystemAgainKeepsTheLastBrightness() = runTest {
        val repository = createRepository()
        repository.setBrightness(0.2f)

        repository.setUseSystemBrightness(true)

        assertEquals(
            ReadingSettings(useSystemBrightness = true, brightness = 0.2f),
            repository.readingSettings.first(),
        )
    }

    @Test
    fun brightnessIsKeptWithinTheVisibleRange() = runTest {
        val repository = createRepository()

        repository.setBrightness(0f)
        assertEquals(ReadingSettings.MIN_BRIGHTNESS, repository.readingSettings.first().brightness)

        repository.setBrightness(3f)
        assertEquals(ReadingSettings.MAX_BRIGHTNESS, repository.readingSettings.first().brightness)
    }
}
