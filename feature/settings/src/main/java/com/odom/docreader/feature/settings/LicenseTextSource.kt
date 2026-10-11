package com.odom.docreader.feature.settings

import android.content.Context
import com.odom.docreader.core.files.di.IoDispatcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface LicenseTextSource {
    suspend fun load(licenseAsset: String): String
}

class AssetLicenseTextSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LicenseTextSource {
    override suspend fun load(licenseAsset: String): String = withContext(ioDispatcher) {
        context.assets.open("$LICENSES_DIR/$licenseAsset").bufferedReader().use { it.readText() }
    }

    private companion object {
        const val LICENSES_DIR = "licenses"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    abstract fun bindLicenseTextSource(source: AssetLicenseTextSource): LicenseTextSource
}
