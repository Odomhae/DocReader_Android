package com.odom.docreader.viewer.pdf.legacy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.odom.docreader.core.files.di.IoDispatcher
import com.odom.docreader.viewer.pdf.PdfLoadFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class PdfRendererDocumentSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PdfDocumentSource {

    override suspend fun open(uri: String): PdfOpenResult = withContext(ioDispatcher) {
        val descriptor = openDescriptor(uri) ?: return@withContext PdfOpenResult.Failed(PdfLoadFailure.UNREADABLE)
        try {
            PdfOpenResult.Opened(RendererPages(PdfRenderer(descriptor), ioDispatcher))
        } catch (e: SecurityException) {
            // PdfRenderer signals a password-protected document this way.
            descriptor.close()
            PdfOpenResult.Failed(PdfLoadFailure.PASSWORD_UNSUPPORTED)
        } catch (e: IOException) {
            descriptor.close()
            // API 26's PdfRenderer reports an encrypted document as a plain IOException.
            val failure = if (looksEncrypted(uri)) PdfLoadFailure.PASSWORD_UNSUPPORTED else PdfLoadFailure.UNREADABLE
            PdfOpenResult.Failed(failure)
        }
    }

    /**
     * An encrypted PDF names its encryption dictionary in a trailer, which sits at the end of the
     * file, or near the start when the file is linearized.
     */
    private fun looksEncrypted(uri: String): Boolean = try {
        context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { input ->
                val channel = input.channel
                val head = ByteArray(minOf(TRAILER_SCAN_BYTES.toLong(), channel.size()).toInt())
                channel.read(ByteBuffer.wrap(head), 0)
                val tail = ByteArray(head.size)
                channel.read(ByteBuffer.wrap(tail), channel.size() - tail.size)
                containsEncryptKey(head) || containsEncryptKey(tail)
            }
        } ?: false
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }

    private fun openDescriptor(uri: String): ParcelFileDescriptor? = try {
        context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")
    } catch (e: FileNotFoundException) {
        null
    } catch (e: SecurityException) {
        null
    }

    private companion object {
        const val TRAILER_SCAN_BYTES = 8 * 1024
    }

    private class RendererPages(
        private val renderer: PdfRenderer,
        private val ioDispatcher: CoroutineDispatcher,
    ) : PdfPages {
        // PdfRenderer allows one open page at a time and must not be closed mid-render.
        private val lock = Any()
        private var closed = false

        override val pageCount: Int = renderer.pageCount

        override suspend fun render(pageIndex: Int, widthPx: Int): Bitmap? = withContext(ioDispatcher) {
            synchronized(lock) {
                if (closed) return@synchronized null
                try {
                    renderer.openPage(pageIndex).use { page ->
                        val heightPx = (widthPx.toLong() * page.height / page.width).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                        // PDF pages have no background of their own.
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                } catch (e: IllegalStateException) {
                    // Thrown for a page the platform renderer cannot parse; the viewer shows a per-page notice.
                    null
                }
            }
        }

        override fun close() {
            synchronized(lock) {
                if (!closed) {
                    closed = true
                    renderer.close()
                }
            }
        }
    }
}

private val ENCRYPT_KEY = "/Encrypt".toByteArray(Charsets.US_ASCII)

internal fun containsEncryptKey(bytes: ByteArray): Boolean =
    (0..bytes.size - ENCRYPT_KEY.size).any { start -> ENCRYPT_KEY.indices.all { bytes[start + it] == ENCRYPT_KEY[it] } }
