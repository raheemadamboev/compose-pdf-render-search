package xyz.teamgravity.compose_pdf_render_search

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfConverter(
    private val context: Context
) {

    private var renderer: PdfRenderer? = null

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    suspend fun convert(contentUri: Uri): ImmutableList<Bitmap> {
        return withContext(Dispatchers.IO) {
            renderer?.close()

            context
                .contentResolver
                .openFileDescriptor(contentUri, "r")
                ?.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    this@PdfConverter.renderer = renderer
                    return@withContext (0 until renderer.pageCount).map { index ->
                        renderer.openPage(index).use pageUse@{ page ->
                            val bitmap = Bitmap.createBitmap(
                                page.width,
                                page.height,
                                Bitmap.Config.ARGB_8888
                            )

                            val canvas = Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            canvas.drawBitmap(bitmap, 0F, 0F, null)

                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )

                            return@pageUse bitmap
                        }
                    }.toImmutableList()
                }

            return@withContext persistentListOf()
        }
    }

    suspend fun search(query: String): ImmutableMap<Int, ImmutableList<RectF>> { // page | list of coordinates
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return persistentMapOf()
        if (query.isBlank()) return persistentMapOf()
        return withContext(Dispatchers.Default) {
            val renderer = renderer ?: return@withContext persistentMapOf()
            return@withContext (0 until renderer.pageCount).associate { index ->
                return@associate renderer.openPage(index).use { page ->
                    val result = page.searchText(query)
                    return@use index to result.map { it.bounds.first() }.toImmutableList()
                }
            }.toImmutableMap()
        }
    }
}