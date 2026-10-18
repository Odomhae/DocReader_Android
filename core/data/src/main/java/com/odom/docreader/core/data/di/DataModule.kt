package com.odom.docreader.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.odom.docreader.core.data.DataStoreLaunchHistoryRepository
import com.odom.docreader.core.data.DataStoreReadingSettingsRepository
import com.odom.docreader.core.data.DataStoreSettingsRepository
import com.odom.docreader.core.data.LaunchHistoryRepository
import com.odom.docreader.core.data.LibraryRepository
import com.odom.docreader.core.data.ReadingSettingsRepository
import com.odom.docreader.core.data.RoomLibraryRepository
import com.odom.docreader.core.data.SettingsRepository
import com.odom.docreader.core.data.db.DocReaderDatabase
import com.odom.docreader.core.data.db.DocumentDao
import com.odom.docreader.core.files.di.IoDispatcher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindLibraryRepository(repository: RoomLibraryRepository): LibraryRepository

    @Binds
    abstract fun bindSettingsRepository(repository: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindReadingSettingsRepository(repository: DataStoreReadingSettingsRepository): ReadingSettingsRepository

    @Binds
    abstract fun bindLaunchHistoryRepository(repository: DataStoreLaunchHistoryRepository): LaunchHistoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): DocReaderDatabase =
            Room.databaseBuilder(context, DocReaderDatabase::class.java, "docreader.db").build()

        @Provides
        fun provideDocumentDao(database: DocReaderDatabase): DocumentDao = database.documentDao()

        @Provides
        @Singleton
        fun provideSettingsDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
        ) { context.preferencesDataStoreFile("settings") }

        @Provides
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
