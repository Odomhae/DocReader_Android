package com.odom.docreader.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.odom.docreader.core.model.SortOrder
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    /** SAF tree Uri of the folder the user picked, or null before the first pick. */
    val folderUri: Flow<String?>

    val sortOrder: Flow<SortOrder>

    suspend fun setFolderUri(uri: String)

    suspend fun setSortOrder(sortOrder: SortOrder)
}

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val folderUri: Flow<String?> =
        dataStore.data.map { it[FOLDER_URI] }.distinctUntilChanged()

    override val sortOrder: Flow<SortOrder> =
        dataStore.data
            .map { preferences ->
                SortOrder.entries.firstOrNull { it.name == preferences[SORT_ORDER] } ?: SortOrder.NAME
            }
            .distinctUntilChanged()

    override suspend fun setFolderUri(uri: String) {
        dataStore.edit { it[FOLDER_URI] = uri }
    }

    override suspend fun setSortOrder(sortOrder: SortOrder) {
        dataStore.edit { it[SORT_ORDER] = sortOrder.name }
    }

    private companion object {
        val FOLDER_URI = stringPreferencesKey("folder_uri")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }
}
