package com.odom.docreader.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.odom.docreader.core.model.FontScale
import com.odom.docreader.core.model.SortOrder
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val sortOrder: Flow<SortOrder>

    val fontScale: Flow<FontScale>

    suspend fun setSortOrder(sortOrder: SortOrder)

    suspend fun setFontScale(fontScale: FontScale)
}

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val sortOrder: Flow<SortOrder> =
        dataStore.data
            .map { preferences ->
                SortOrder.entries.firstOrNull { it.name == preferences[SORT_ORDER] } ?: SortOrder.NAME
            }
            .distinctUntilChanged()

    override val fontScale: Flow<FontScale> =
        dataStore.data
            .map { preferences ->
                FontScale.entries.firstOrNull { it.name == preferences[FONT_SCALE] } ?: FontScale.NORMAL
            }
            .distinctUntilChanged()

    override suspend fun setSortOrder(sortOrder: SortOrder) {
        dataStore.edit { it[SORT_ORDER] = sortOrder.name }
    }

    override suspend fun setFontScale(fontScale: FontScale) {
        dataStore.edit { it[FONT_SCALE] = fontScale.name }
    }

    private companion object {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val FONT_SCALE = stringPreferencesKey("font_scale")
    }
}
