package com.odom.docreader.viewer.web.di

import com.odom.docreader.viewer.web.SystemWebViewVersionProvider
import com.odom.docreader.viewer.web.WebViewVersionProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WebViewerModule {
    @Binds
    abstract fun bindWebViewVersionProvider(provider: SystemWebViewVersionProvider): WebViewVersionProvider
}
