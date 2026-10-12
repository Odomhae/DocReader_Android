package com.odom.docreader.core.files.di

import com.odom.docreader.core.files.ContentExternalDocumentResolver
import com.odom.docreader.core.files.DocumentScanner
import com.odom.docreader.core.files.ExternalDocumentResolver
import com.odom.docreader.core.files.MediaStoreDocumentScanner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
abstract class FilesModule {
    @Binds
    abstract fun bindDocumentScanner(scanner: MediaStoreDocumentScanner): DocumentScanner

    @Binds
    abstract fun bindExternalDocumentResolver(resolver: ContentExternalDocumentResolver): ExternalDocumentResolver

    companion object {
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
