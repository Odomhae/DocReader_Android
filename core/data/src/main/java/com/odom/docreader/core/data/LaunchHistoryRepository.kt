package com.odom.docreader.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject

interface LaunchHistoryRepository {
    /** Records this launch and returns true if the app had never been launched before. */
    suspend fun recordLaunch(): Boolean
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

    private companion object {
        val LAUNCHED_BEFORE = booleanPreferencesKey("launched_before")
    }
}
