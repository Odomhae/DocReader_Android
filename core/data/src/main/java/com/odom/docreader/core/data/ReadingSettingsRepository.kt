package com.odom.docreader.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.odom.docreader.core.model.PaperColor
import com.odom.docreader.core.model.ReadingSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface ReadingSettingsRepository {
    val readingSettings: Flow<ReadingSettings>

    suspend fun setPaperColor(paperColor: PaperColor)

    suspend fun setUseSystemBrightness(useSystemBrightness: Boolean)

    /** Also stops following the system brightness. */
    suspend fun setBrightness(brightness: Float)
}

class DataStoreReadingSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ReadingSettingsRepository {

    override val readingSettings: Flow<ReadingSettings> =
        dataStore.data
            .map { preferences ->
                ReadingSettings(
                    paperColor = PaperColor.entries.firstOrNull { it.name == preferences[PAPER_COLOR] }
                        ?: PaperColor.WHITE,
                    useSystemBrightness = preferences[USE_SYSTEM_BRIGHTNESS] ?: true,
                    brightness = (preferences[BRIGHTNESS] ?: ReadingSettings.DEFAULT_BRIGHTNESS).coerceToVisible(),
                )
            }
            .distinctUntilChanged()

    override suspend fun setPaperColor(paperColor: PaperColor) {
        dataStore.edit { it[PAPER_COLOR] = paperColor.name }
    }

    override suspend fun setUseSystemBrightness(useSystemBrightness: Boolean) {
        dataStore.edit { it[USE_SYSTEM_BRIGHTNESS] = useSystemBrightness }
    }

    override suspend fun setBrightness(brightness: Float) {
        dataStore.edit {
            it[BRIGHTNESS] = brightness.coerceToVisible()
            it[USE_SYSTEM_BRIGHTNESS] = false
        }
    }

    private fun Float.coerceToVisible(): Float =
        coerceIn(ReadingSettings.MIN_BRIGHTNESS, ReadingSettings.MAX_BRIGHTNESS)

    private companion object {
        val PAPER_COLOR = stringPreferencesKey("paper_color")
        val USE_SYSTEM_BRIGHTNESS = booleanPreferencesKey("use_system_brightness")
        val BRIGHTNESS = floatPreferencesKey("brightness")
    }
}
