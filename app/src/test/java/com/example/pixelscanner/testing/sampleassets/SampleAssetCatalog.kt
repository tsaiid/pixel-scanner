package com.example.pixelscanner.testing.sampleassets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Shader
import android.net.Uri
import com.example.pixelscanner.core.model.ScanSession
import com.example.pixelscanner.core.model.ScannedPage
import com.example.pixelscanner.core.model.SessionDirtyFlags
import java.io.File
import java.io.FileOutputStream

data class SampleImageAsset(
    val name: String,
    val file: File,
    val width: Int,
    val height: Int,
) {
    val uri: Uri = Uri.fromFile(file)
}

data class SampleDocumentAsset(
    val name: String,
    val pages: List<SampleImageAsset>,
) {
    val pageCount: Int get() = pages.size
    val pageUris: List<Uri> get() = pages.map { it.uri }
}

data class SampleAssetSuite(
    val singlePageClean: SampleImageAsset,
    val multiPageSix: SampleDocumentAsset,
    val multiPageTwelve: SampleDocumentAsset,
    val multiPageTwentyFour: SampleDocumentAsset,
    val lowLightPage: SampleImageAsset,
    val shadowPage: SampleImageAsset,
    val rotatedPage: SampleImageAsset,
    val highResPage: SampleImageAsset,
    val longDocumentFortyEight: SampleDocumentAsset,
    val longDocumentSeventyTwo: SampleDocumentAsset,
)

object SampleAssetCatalog {
    private const val ROOT_DIR = "integration-sample-assets"
    private const val JPEG_QUALITY = 92

    fun suite(context: Context): SampleAssetSuite {
        return SampleAssetSuite(
            singlePageClean = ensureImage(
                context = context,
                relativeName = "pbm-sp-1p-1600x2200-orig.jpg",
                width = 1600,
                height = 2200,
                style = SamplePageStyle.CLEAN,
            ),
            multiPageSix = ensureDocument(
                context = context,
                relativeName = "pbm-mp-6p-1200x1600-mixed",
                pageCount = 6,
                width = 1200,
                height = 1600,
                style = SamplePageStyle.MIXED,
            ),
            multiPageTwelve = ensureDocument(
                context = context,
                relativeName = "pbm-mp-12p-1200x1600-mixed",
                pageCount = 12,
                width = 1200,
                height = 1600,
                style = SamplePageStyle.MIXED,
            ),
            multiPageTwentyFour = ensureDocument(
                context = context,
                relativeName = "pbm-mp-24p-1200x1600-mixed",
                pageCount = 24,
                width = 1200,
                height = 1600,
                style = SamplePageStyle.MIXED,
            ),
            lowLightPage = ensureImage(
                context = context,
                relativeName = "pbm-lowlight-1p-1200x1600-low.jpg",
                width = 1200,
                height = 1600,
                style = SamplePageStyle.LOW_LIGHT,
            ),
            shadowPage = ensureImage(
                context = context,
                relativeName = "pbm-shadow-1p-1200x1600-shadow.jpg",
                width = 1200,
                height = 1600,
                style = SamplePageStyle.SHADOW,
            ),
            rotatedPage = ensureImage(
                context = context,
                relativeName = "pbm-rot-1p-1200x1600-rot.jpg",
                width = 1200,
                height = 1600,
                style = SamplePageStyle.ROTATED,
            ),
            highResPage = ensureImage(
                context = context,
                relativeName = "pbm-large-1p-2400x3600-orig.jpg",
                width = 2400,
                height = 3600,
                style = SamplePageStyle.HIGH_RES,
            ),
            longDocumentFortyEight = ensureDocument(
                context = context,
                relativeName = "pbm-long-48p-900x1200-mixed",
                pageCount = 48,
                width = 900,
                height = 1200,
                style = SamplePageStyle.MIXED,
            ),
            longDocumentSeventyTwo = ensureDocument(
                context = context,
                relativeName = "pbm-long-72p-900x1200-mixed",
                pageCount = 72,
                width = 900,
                height = 1200,
                style = SamplePageStyle.MIXED,
            ),
        )
    }

    fun reviewStressSession(
        context: Context,
        pageCount: Int,
    ): ScanSession {
        val document = ensureDocument(
            context = context,
            relativeName = "pbm-long-${pageCount}p-900x1200-mixed",
            pageCount = pageCount,
            width = 900,
            height = 1200,
            style = SamplePageStyle.MIXED,
        )

        return ScanSession(
            id = "sample-review-stress-$pageCount",
            pages = document.pages.mapIndexed { index, asset ->
                ScannedPage(
                    id = "${document.name}_page_${index + 1}",
                    originalSourceUri = asset.uri,
                )
            },
            dirtyFlags = SessionDirtyFlags(hasReorderedPages = true),
        )
    }

    private fun ensureImage(
        context: Context,
        relativeName: String,
        width: Int,
        height: Int,
        style: SamplePageStyle,
    ): SampleImageAsset {
        val root = sampleRoot(context)
        val file = File(root, relativeName)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            renderImage(file = file, width = width, height = height, style = style, pageLabel = relativeName)
        }
        return SampleImageAsset(
            name = relativeName,
            file = file,
            width = width,
            height = height,
        )
    }

    private fun ensureDocument(
        context: Context,
        relativeName: String,
        pageCount: Int,
        width: Int,
        height: Int,
        style: SamplePageStyle,
    ): SampleDocumentAsset {
        val root = sampleRoot(context)
        val documentDir = File(root, relativeName)
        documentDir.mkdirs()

        val cover = File(documentDir, "page-01.jpg")
        if (!cover.exists()) {
            renderImage(
                file = cover,
                width = width,
                height = height,
                style = style,
                pageLabel = relativeName,
            )
        }

        val coverBytes = cover.readBytes()
        val pages = (1..pageCount).map { index ->
            val file = File(documentDir, "page-${index.toString().padStart(2, '0')}.jpg")
            if (!file.exists()) {
                file.writeBytes(coverBytes)
            }
            SampleImageAsset(
                name = file.name,
                file = file,
                width = width,
                height = height,
            )
        }

        return SampleDocumentAsset(
            name = relativeName,
            pages = pages,
        )
    }

    private fun sampleRoot(context: Context): File {
        return File(context.cacheDir, ROOT_DIR).apply {
            mkdirs()
        }
    }

    private fun renderImage(
        file: File,
        width: Int,
        height: Int,
        style: SamplePageStyle,
        pageLabel: String,
    ) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (style) {
            SamplePageStyle.ROTATED -> {
                canvas.save()
                canvas.translate(width / 2f, height / 2f)
                canvas.rotate(-8f)
                canvas.translate(-width / 2f, -height / 2f)
                drawPageLayout(canvas, width, height, style, pageLabel)
                canvas.restore()
            }

            else -> drawPageLayout(canvas, width, height, style, pageLabel)
        }

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        }
        bitmap.recycle()
    }

    private fun drawPageLayout(
        canvas: Canvas,
        width: Int,
        height: Int,
        style: SamplePageStyle,
        pageLabel: String,
    ) {
        val palette = style.palette
        canvas.drawColor(palette.background)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.header
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.body
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.text
            textSize = (width * 0.052f).coerceAtMost(72f)
            typeface = Typeface.MONOSPACE
        }
        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.subText
            textSize = (width * 0.03f).coerceAtMost(42f)
            typeface = Typeface.MONOSPACE
        }

        val margin = width * 0.08f
        val headerHeight = height * 0.16f
        val contentTop = headerHeight + margin

        canvas.drawRoundRect(
            RectF(margin, margin, width - margin, margin + headerHeight),
            42f,
            42f,
            headerPaint,
        )
        canvas.drawRoundRect(
            RectF(margin, contentTop, width - margin, height - margin),
            36f,
            36f,
            bodyPaint,
        )

        if (style == SamplePageStyle.SHADOW || style == SamplePageStyle.MIXED) {
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    contentTop,
                    width.toFloat(),
                    height.toFloat(),
                    Color.argb(0, 0, 0, 0),
                    Color.argb(120, 0, 0, 0),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawRect(
                RectF(margin, contentTop, width - margin, height - margin),
                shadowPaint,
            )
        }

        val title = when (style) {
            SamplePageStyle.CLEAN -> "Clean page"
            SamplePageStyle.LOW_LIGHT -> "Low light page"
            SamplePageStyle.SHADOW -> "Shadow page"
            SamplePageStyle.ROTATED -> "Rotated page"
            SamplePageStyle.HIGH_RES -> "High resolution page"
            SamplePageStyle.MIXED -> "Mixed document page"
        }

        canvas.drawText(title, margin * 1.2f, margin * 2.2f, textPaint)
        canvas.drawText(pageLabel, margin * 1.2f, margin * 2.8f, subTextPaint)
        canvas.drawText("Pixel Scanner sample asset", margin * 1.2f, margin * 3.4f, subTextPaint)

        val blockTop = contentTop + height * 0.08f
        val blockHeight = height * 0.08f
        val blockWidth = width * 0.68f
        repeat(4) { index ->
            val top = blockTop + index * (blockHeight * 1.35f)
            canvas.drawRoundRect(
                RectF(
                    margin * 1.2f,
                    top,
                    margin * 1.2f + blockWidth,
                    top + blockHeight,
                ),
                24f,
                24f,
                if (index % 2 == 0) accentPaint else bodyPaint,
            )
        }

        if (style == SamplePageStyle.LOW_LIGHT) {
            val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(96, 0, 0, 0)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        }

        if (style == SamplePageStyle.ROTATED) {
            val noticePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = (width * 0.028f).coerceAtMost(36f)
            }
            canvas.drawText("rotated capture", margin * 1.2f, height - margin * 0.9f, noticePaint)
        }
    }

    private enum class SamplePageStyle(
        val palette: PagePalette,
    ) {
        CLEAN(
            PagePalette(
                background = Color.WHITE,
                header = Color.rgb(240, 244, 250),
                body = Color.rgb(250, 250, 252),
                accent = Color.rgb(33, 82, 168),
                text = Color.rgb(24, 24, 24),
                subText = Color.rgb(90, 90, 90),
            ),
        ),
        LOW_LIGHT(
            PagePalette(
                background = Color.rgb(34, 34, 38),
                header = Color.rgb(55, 55, 66),
                body = Color.rgb(46, 46, 54),
                accent = Color.rgb(130, 180, 255),
                text = Color.rgb(238, 238, 238),
                subText = Color.rgb(204, 204, 204),
            ),
        ),
        SHADOW(
            PagePalette(
                background = Color.rgb(242, 238, 232),
                header = Color.rgb(250, 246, 242),
                body = Color.rgb(255, 255, 255),
                accent = Color.rgb(106, 62, 24),
                text = Color.rgb(42, 30, 19),
                subText = Color.rgb(92, 78, 64),
            ),
        ),
        ROTATED(
            PagePalette(
                background = Color.rgb(248, 244, 238),
                header = Color.rgb(240, 228, 214),
                body = Color.rgb(253, 250, 246),
                accent = Color.rgb(164, 97, 40),
                text = Color.rgb(42, 32, 24),
                subText = Color.rgb(98, 82, 70),
            ),
        ),
        HIGH_RES(
            PagePalette(
                background = Color.WHITE,
                header = Color.rgb(232, 240, 232),
                body = Color.rgb(252, 253, 252),
                accent = Color.rgb(27, 123, 88),
                text = Color.rgb(24, 40, 28),
                subText = Color.rgb(80, 96, 84),
            ),
        ),
        MIXED(
            PagePalette(
                background = Color.WHITE,
                header = Color.rgb(238, 242, 248),
                body = Color.rgb(250, 251, 253),
                accent = Color.rgb(47, 94, 156),
                text = Color.rgb(28, 28, 28),
                subText = Color.rgb(92, 92, 92),
            ),
        );
    }

    private data class PagePalette(
        val background: Int,
        val header: Int,
        val body: Int,
        val accent: Int,
        val text: Int,
        val subText: Int,
    )
}
