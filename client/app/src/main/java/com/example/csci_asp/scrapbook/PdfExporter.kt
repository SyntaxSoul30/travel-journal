package com.example.csci_asp.scrapbook

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.ContextCompat
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfExporter(
    private val context: Context
) {

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val frameStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#33000000")
        strokeWidth = FRAME_STROKE
    }
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    suspend fun export(
        photoUris: List<Uri>,
        template: ScrapbookTemplate,
        outputStream: OutputStream
    ) = withContext(Dispatchers.IO) {
        require(photoUris.isNotEmpty()) { "Photo list cannot be empty" }

        val document = PdfDocument()
        val aspectRatioCache = mutableMapOf<Uri, Float>()
        try {
            val pages = paginatePhotos(photoUris, template, aspectRatioCache)
            pages.forEachIndexed { index, pagePhotos ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                drawBackground(canvas, template, pageInfo)
                drawPhotos(canvas, pagePhotos, template, pageInfo, aspectRatioCache)
                document.finishPage(page)
            }

            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun drawBackground(
        canvas: Canvas,
        template: ScrapbookTemplate,
        pageInfo: PdfDocument.PageInfo
    ) {
        val background = ContextCompat.getDrawable(context, template.backgroundRes)
        background?.setBounds(0, 0, pageInfo.pageWidth, pageInfo.pageHeight)
        background?.draw(canvas)
    }

    private fun drawPhotos(
        canvas: Canvas,
        photoUris: List<Uri>,
        template: ScrapbookTemplate,
        pageInfo: PdfDocument.PageInfo,
        aspectRatioCache: MutableMap<Uri, Float>
    ) {
        val columns = max(1, template.columns)
        val padding = PAGE_PADDING
        val availableWidth = pageInfo.pageWidth.toFloat() - padding * 2
        val columnWidth = (availableWidth - (SPACING * (columns - 1))) / columns
        val columnHeights = FloatArray(columns) { padding }
        val placements = mutableListOf<PhotoPlacement>()

        photoUris.forEachIndexed { index, uri ->
            // Get photo aspect ratio
            val aspectRatio = aspectRatioCache.getOrPut(uri) { resolveAspectRatio(uri) }
                .takeIf { it > 0f && it.isFinite() } ?: 1f // Default to 1:1 if dimensions not available

            val safeAspect = if (aspectRatio <= 0f) 1f else aspectRatio
            val columnIndex = index % columns
            val left = padding + columnIndex * (columnWidth + SPACING)
            val top = columnHeights[columnIndex]
            val cellWidth = columnWidth
            val cellHeight = columnWidth / safeAspect
            val right = left + cellWidth
            val bottom = top + cellHeight

            val inset = PHOTO_SPACING / 2f
            val insetRect = RectF(
                left + inset,
                top + inset,
                right - inset,
                bottom - inset
            )

            placements += PhotoPlacement(uri, insetRect)
            columnHeights[columnIndex] = bottom + PHOTO_SPACING + SPACING
        }

        val contentBottom = columnHeights.maxOrNull()?.minus(SPACING) ?: padding
        val contentHeight = max(1f, contentBottom - padding)
        val availableHeight = pageInfo.pageHeight - padding * 2
        val scale = if (contentHeight > availableHeight) {
            availableHeight / contentHeight
        } else {
            1f
        }

        placements.forEachIndexed { index, placement ->
            val scaledRect = if (scale < 1f) {
                scaleRectFromPadding(placement.rect, padding, availableWidth, scale)
            } else {
                placement.rect
            }

            val targetWidth = scaledRect.width()
            val targetHeight = scaledRect.height()

            val targetWidthPx = min(MAX_BITMAP_DIMENSION, max(1, targetWidth.toInt()))
            val targetHeightPx = min(MAX_BITMAP_DIMENSION, max(1, targetHeight.toInt()))

            val bitmap = decodeScaledBitmap(
                placement.uri,
                targetWidthPx,
                targetHeightPx
            ) ?: return@forEachIndexed

            val frameRect = RectF(scaledRect)
            val photoRect = RectF(scaledRect)

            val bitmapScale = min(
                photoRect.width() / bitmap.width.toFloat(),
                photoRect.height() / bitmap.height.toFloat()
            )
            val scaledWidth = bitmap.width * bitmapScale
            val scaledHeight = bitmap.height * bitmapScale
            val bitmapLeft = photoRect.left + (photoRect.width() - scaledWidth) / 2
            val bitmapTop = photoRect.top + (photoRect.height() - scaledHeight) / 2
            val bitmapRect = RectF(
                bitmapLeft,
                bitmapTop,
                bitmapLeft + scaledWidth,
                bitmapTop + scaledHeight
            )

            val angle = template.rotationFor(index)
            val centerX = frameRect.centerX()
            val centerY = frameRect.centerY()
            canvas.save()
            canvas.rotate(angle, centerX, centerY)
            canvas.drawRoundRect(frameRect, FRAME_RADIUS, FRAME_RADIUS, framePaint)
            canvas.drawRoundRect(frameRect, FRAME_RADIUS, FRAME_RADIUS, frameStrokePaint)
            canvas.save()
            val clipPath = Path().apply {
                addRoundRect(bitmapRect, PHOTO_CORNER_RADIUS, PHOTO_CORNER_RADIUS, Path.Direction.CW)
            }
            canvas.clipPath(clipPath)
            canvas.drawBitmap(bitmap, null, bitmapRect, bitmapPaint)
            canvas.restore()
            canvas.restore()
            bitmap.recycle()
        }
    }

    /**
     * Gets image dimensions without loading the full bitmap
     */
    private fun getImageDimensions(uri: Uri): Pair<Int, Int>? {
        val resolver: ContentResolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        return try {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    options.outWidth to options.outHeight
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeScaledBitmap(
        uri: Uri,
        width: Int,
        height: Int
    ): Bitmap? {
        val resolver: ContentResolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
        options.inSampleSize = calculateInSampleSize(options, width, height)
        options.inJustDecodeBounds = false
        return try {
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private data class PhotoPlacement(
        val uri: Uri,
        val rect: RectF
    )

    private fun scaleRectFromPadding(
        rect: RectF,
        padding: Float,
        availableWidth: Float,
        scale: Float
    ): RectF {
        val horizontalOffset = (availableWidth - availableWidth * scale) / 2f
        val newLeft = padding + horizontalOffset + (rect.left - padding) * scale
        val newTop = padding + (rect.top - padding) * scale
        val newRight = newLeft + rect.width() * scale
        val newBottom = newTop + rect.height() * scale
        return RectF(newLeft, newTop, newRight, newBottom)
    }

    private fun paginatePhotos(
        photoUris: List<Uri>,
        template: ScrapbookTemplate,
        aspectRatioCache: MutableMap<Uri, Float>
    ): List<List<Uri>> {
        if (photoUris.isEmpty()) return emptyList()

        val columns = max(1, template.columns)
        val padding = PAGE_PADDING
        val availableWidth = PAGE_WIDTH.toFloat() - padding * 2
        val columnWidth = (availableWidth - (SPACING * (columns - 1))) / columns
        val maxContentHeight = PAGE_HEIGHT.toFloat() - padding * 2
        val columnHeights = FloatArray(columns) { padding }

        val pages = mutableListOf<List<Uri>>()
        val currentPage = mutableListOf<Uri>()
        var pageIndex = 0

        fun resetColumns() {
            for (i in columnHeights.indices) {
                columnHeights[i] = padding
            }
        }

        fun flushPage() {
            if (currentPage.isNotEmpty()) {
                pages += currentPage.toList()
                currentPage.clear()
            }
            resetColumns()
            pageIndex = 0
        }

        photoUris.forEach { uri ->
            while (true) {
                val aspectRatio = aspectRatioCache.getOrPut(uri) { resolveAspectRatio(uri) }
                val safeAspect = aspectRatio.takeIf { it > 0f && it.isFinite() } ?: 1f
                val columnIndex = if (columns == 0) 0 else pageIndex % columns
                val top = columnHeights[columnIndex]
                val cellHeight = columnWidth / safeAspect
                val bottom = top + cellHeight
                val updatedColumnHeight = bottom + PHOTO_SPACING + SPACING

                var potentialMax = updatedColumnHeight
                for (i in columnHeights.indices) {
                    if (i == columnIndex) continue
                    potentialMax = max(potentialMax, columnHeights[i])
                }

                val contentBottom = (potentialMax - SPACING).coerceAtLeast(padding)
                val contentHeight = max(1f, contentBottom - padding)

                if (contentHeight > maxContentHeight && currentPage.isNotEmpty()) {
                    flushPage()
                    continue
                }

                currentPage += uri
                columnHeights[columnIndex] = updatedColumnHeight
                pageIndex++
                break
            }
        }

        flushPage()
        return pages
    }

    companion object {
        private const val PAGE_WIDTH = 1240
        private const val PAGE_HEIGHT = 1754
        private const val SPACING = 32f
        private const val PAGE_PADDING = 72f
        private const val FRAME_RADIUS = 36f
        private const val FRAME_STROKE = 6f
        private const val PHOTO_SPACING = 16f
        private const val PHOTO_CORNER_RADIUS = 32f
        private const val MAX_BITMAP_DIMENSION = 1080
    }

    private fun resolveAspectRatio(uri: Uri): Float {
        val dimensions = getImageDimensions(uri) ?: return 1f
        val width = dimensions.first
        val height = dimensions.second
        if (width <= 0 || height <= 0) return 1f
        return width.toFloat() / height.toFloat()
    }

    private fun ScrapbookTemplate.rotationFor(index: Int): Float =
        if (rotationPattern.isEmpty()) 0f else rotationPattern[index % rotationPattern.size]
}

