package com.odom.docreader.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreLaunchHistoryRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun TestScope.createRepository(): DataStoreLaunchHistoryRepository {
        val file = temporaryFolder.root.resolve("settings.preferences_pb")
        return DataStoreLaunchHistoryRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
    }

    @Test
    fun onlyTheFirstLaunchIsReportedAsFirst() = runTest {
        val repository = createRepository()

        assertTrue(repository.recordLaunch())
        assertFalse(repository.recordLaunch())
        assertFalse(repository.recordLaunch())
    }
}
