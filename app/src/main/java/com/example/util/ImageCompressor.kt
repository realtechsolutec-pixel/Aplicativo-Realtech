package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.util.Locale

enum class ImageCompressionPreset(
    val code: String,
    val label: String,
    val shortLabel: String,
    val description: String,
    val maxDimensionPx: Int,
    val targetMaxBytes: Int,
    val initialQuality: Int,
    val minQuality: Int
) {
    ECONOMIA_MAXIMA(
        code = "ECONOMIA_MAXIMA",
        label = "Máxima (até 65 KB)",
        shortLabel = "Máxima (65 KB)",
        description = "720px • WebP ultra leve para economizar espaço no Supabase",
        maxDimensionPx = 720,
        targetMaxBytes = 65 * 1024,
        initialQuality = 62,
        minQuality = 30
    ),
    EQUILIBRADO(
        code = "EQUILIBRADO",
        label = "Equilibrado (até 120 KB)",
        shortLabel = "Equilibrado (120 KB)",
        description = "1024px • WebP recomendado: ótima nitidez e arquivo pequeno",
        maxDimensionPx = 1024,
        targetMaxBytes = 120 * 1024,
        initialQuality = 74,
        minQuality = 38
    ),
    ALTA_NITIDEZ(
        code = "ALTA_NITIDEZ",
        label = "Alta Nitidez (até 240 KB)",
        shortLabel = "Nitidez (240 KB)",
        description = "1440px • WebP ideal para zoom em placas e componentes SMD",
        maxDimensionPx = 1440,
        targetMaxBytes = 240 * 1024,
        initialQuality = 82,
        minQuality = 50
    );

    companion object {
        fun fromCode(code: String?): ImageCompressionPreset =
            entries.find { it.code == code } ?: EQUILIBRADO
    }
}

data class CompressedImageResult(
    val bytes: ByteArray,
    val mimeType: String,
    val extension: String,
    val originalBytes: Long,
    val compressedBytes: Int,
    val width: Int,
    val height: Int
) {
    val savedBytes: Long
        get() = (originalBytes - compressedBytes).coerceAtLeast(0L)

    val savedPercent: Int
        get() = if (originalBytes > 0L) {
            (((originalBytes - compressedBytes).toDouble() / originalBytes.toDouble()) * 100.0)
                .toInt()
                .coerceIn(0, 99)
        } else {
            0
        }
}

object ImageCompressor {

    private fun webpFormat(): Bitmap.CompressFormat {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }

    fun getUriSizeBytes(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                if (afd.length >= 0L) afd.length else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }.let { fdLen ->
            if (fdLen > 0L) fdLen else try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    var total = 0L
                    val buf = ByteArray(8192)
                    while (true) {
                        val read = stream.read(buf)
                        if (read <= 0) break
                        total += read
                    }
                    total
                } ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }

    fun compressUri(
        context: Context,
        uri: Uri,
        preset: ImageCompressionPreset = ImageCompressionPreset.EQUILIBRADO
    ): CompressedImageResult? {
        return try {
            val resolver = context.contentResolver
            val originalBytes = getUriSizeBytes(context, uri)

            val rotationDegrees = readExifRotationDegrees(context, uri)

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            var sampleSize = 1
            while (
                boundsOpts.outWidth / sampleSize > preset.maxDimensionPx * 2 ||
                boundsOpts.outHeight / sampleSize > preset.maxDimensionPx * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val oriented = applyRotationIfNeeded(decoded, rotationDegrees)
            if (oriented !== decoded) decoded.recycle()

            var working = scaleDownIfNeeded(oriented, preset.maxDimensionPx)
            if (working !== oriented) oriented.recycle()

            val format = webpFormat()
            val out = ByteArrayOutputStream()
            var quality = preset.initialQuality
            working.compress(format, quality, out)

            while (out.size() > preset.targetMaxBytes && quality > preset.minQuality) {
                quality = (quality - 8).coerceAtLeast(preset.minQuality)
                out.reset()
                working.compress(format, quality, out)
            }

            // Se ainda estiver acima do limite desejado, reduz resolução em 20% e recomprime
            if (out.size() > preset.targetMaxBytes && (working.width > 480 || working.height > 480)) {
                val smaller = Bitmap.createScaledBitmap(
                    working,
                    (working.width * 0.8f).toInt().coerceAtLeast(1),
                    (working.height * 0.8f).toInt().coerceAtLeast(1),
                    true
                )
                if (smaller !== working) {
                    working.recycle()
                    working = smaller
                }
                out.reset()
                working.compress(format, preset.minQuality, out)
            }

            val finalWidth = working.width
            val finalHeight = working.height
            val bytes = out.toByteArray()
            working.recycle()

            val effectiveOriginal = if (originalBytes > 0L) originalBytes else bytes.size.toLong()
            CompressedImageResult(
                bytes = bytes,
                mimeType = "image/webp",
                extension = "webp",
                originalBytes = effectiveOriginal,
                compressedBytes = bytes.size,
                width = finalWidth,
                height = finalHeight
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun readExifRotationDegrees(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun applyRotationIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        return Bitmap.createScaledBitmap(
            bitmap,
            (w * ratio).toInt().coerceAtLeast(1),
            (h * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale("pt", "BR"), "%.0f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale("pt", "BR"), "%.2f MB", mb)
    }
}
