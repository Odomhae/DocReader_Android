package com.odom.docreader.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.odom.docreader.core.model.SortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        assertNull(repository.folderUri.first())
        assertEquals(SortOrder.NAME, repository.sortOrder.first())
    }

    @Test
    fun savedValuesAreReadBack() = runTest {
        val repository = createRepository()

        repository.setFolderUri("content://tree/docs")
        repository.setSortOrder(SortOrder.SIZE)

        assertEquals("content://tree/docs", repository.folderUri.first())
        assertEquals(SortOrder.SIZE, repository.sortOrder.first())
    }
}
