package codes.tashif.paisa.ui.statement

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Capture file plus the JPEG we hand to the bill reader.
 *
 * On Android 9 and later the decode goes through [ImageDecoder], which is the
 * platform path Pixel uses for HEIF and Ultra HDR. A Pixel JPEG with a gainmap
 * stays a gainmap bitmap, and [Bitmap.compress] writes that gainmap back out
 * on Android 14 and later. Older devices fall back to [BitmapFactory].
 */
internal object BillPhoto {
    private const val MAX_EDGE = 1600
    private const val JPEG_QUALITY = 92

    fun newCaptureFile(context: Context): File {
        val dir = File(context.cacheDir, "bill-photos").apply { mkdirs() }
        return File(dir, "capture-${System.currentTimeMillis()}.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Rewrites [uri] as an upright JPEG no wider or taller than 1600px.
     * Returns null when the file isn't a readable image.
     */
    fun prepare(context: Context, uri: Uri): Uri? {
        val bitmap = decode(context, uri) ?: return null
        val dir = File(context.cacheDir, "bill-photos").apply { mkdirs() }
        val out = File(dir, "bill-${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        bitmap.recycle()
        return uriFor(context, out)
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            decodeWithImageDecoder(context, uri)?.let { return it }
        }
        return decodeWithBitmapFactory(context, uri)
    }

    private fun decodeWithImageDecoder(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT < 28) return null
        return runCatching {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = info.size.width
                val height = info.size.height
                val longest = maxOf(width, height)
                if (longest > MAX_EDGE) {
                    val scale = MAX_EDGE.toFloat() / longest
                    decoder.setTargetSize(
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        }.getOrNull()
    }

    private fun decodeWithBitmapFactory(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sample = sampleSize(bounds.outWidth, bounds.outHeight)
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val upright = applyExif(context, uri, decoded)
        val scaled = scaleDown(upright)
        if (scaled !== upright && !upright.isRecycled) upright.recycle()
        return scaled
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var size = 1
        while (width / size > MAX_EDGE * 2 || height / size > MAX_EDGE * 2) size *= 2
        return size
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_EDGE) return bitmap
        val scale = MAX_EDGE.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun applyExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val rotation = context.contentResolver.openInputStream(uri)?.use { input ->
            when (ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }
}
