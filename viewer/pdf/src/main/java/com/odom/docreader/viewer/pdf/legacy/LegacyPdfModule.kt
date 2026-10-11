package com.odom.docreader.viewer.pdf.legacy

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LegacyPdfModule {
    @Binds
    abstract fun bindPdfDocumentSource(source: PdfRendererDocumentSource): PdfDocumentSource
}
