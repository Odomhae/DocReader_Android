package com.odom.docreader.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import javax.inject.Inject
import kotlinx.coroutines.flow.first

interface LaunchHistoryRepository {
    /** Records this launch and returns true if the app had never been launched before. */
    suspend fun recordLaunch(): Boolean

    /** Wall-clock time of the last app open ad, or null if none was ever shown. */
    suspend fun lastAppOpenAdAt(): Long?

    suspend fun recordAppOpenAd(atMillis: Long)
}

class DataStoreLaunchHistoryRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : LaunchHistoryRepository {

    override suspend fun recordLaunch(): Boolean {
        var firstLaunch = false
        dataStore.edit { preferences ->
            firstLaunch = preferences[LAUNCHED_BEFORE] != true
            preferences[LAUNCHED_BEFORE] = true
        }
        return firstLaunch
    }

    override suspend fun lastAppOpenAdAt(): Long? = dataStore.data.first()[LAST_APP_OPEN_AD_AT]

    override suspend fun recordAppOpenAd(atMillis: Long) {
        dataStore.edit { preferences -> preferences[LAST_APP_OPEN_AD_AT] = atMillis }
    }

    private companion object {
        val LAUNCHED_BEFORE = booleanPreferencesKey("launched_before")
        val LAST_APP_OPEN_AD_AT = longPreferencesKey("last_app_open_ad_at")
    }
}
