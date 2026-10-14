package com.odom.docreader.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.odom.docreader.core.model.FontScale
import com.odom.docreader.core.model.SortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.createRepository(): DataStoreSettingsRepository {
        val file = temporaryFolder.root.resolve("settings.preferences_pb")
        return DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
    }

    @Test
    fun defaultsBeforeAnythingIsSaved() = runTest {
        val repository = createRepository()

        assertEquals(SortOrder.NAME, repository.sortOrder.first())
        assertEquals(FontScale.NORMAL, repository.fontScale.first())
    }

    @Test
    fun savedValuesAreReadBack() = runTest {
        val repository = createRepository()

        repository.setSortOrder(SortOrder.SIZE)
        repository.setFontScale(FontScale.LARGE)

        assertEquals(SortOrder.SIZE, repository.sortOrder.first())
        assertEquals(FontScale.LARGE, repository.fontScale.first())
    }
}
