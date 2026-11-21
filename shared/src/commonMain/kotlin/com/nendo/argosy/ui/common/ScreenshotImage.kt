package com.nendo.argosy.ui.common

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import com.nendo.argosy.platform.asImageBitmap
import com.nendo.argosy.platform.config
import com.nendo.argosy.platform.copy
import com.nendo.argosy.platform.getPixel
import com.nendo.argosy.platform.height
import com.nendo.argosy.platform.recycle
import com.nendo.argosy.platform.setPixels
import com.nendo.argosy.platform.width
import com.nendo.argosy.platform.PlatformBitmaps
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.nendo.argosy.platform.LocalPlatformContext as LocalContext
import androidx.compose.ui.unit.IntRect
import com.nendo.argosy.platform.image.AsyncImage
import com.nendo.argosy.platform.image.bitmap
import com.nendo.argosy.platform.image.intrinsicHeight
import com.nendo.argosy.platform.image.intrinsicWidth
import com.nendo.argosy.platform.image.ImageRequest
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A game screenshot or save-state frame filling its slot.
 *
 * A dual-screen frame captured as both screens stacked (DS 2:3 or 3DS 5:6 in shape) shows only
 * its top screen in a landscape slot, and both screens fitted whole in a portrait or square slot.
 * Every other image fills the slot center-cropped.
 */
@Composable
fun ScreenshotImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onError: ((Any) -> Unit)? = null
) {
    val context = LocalContext.current
    BoxWithConstraints(modifier = modifier) {
        val slotAspect = if (constraints.hasBoundedWidth && constraints.hasBoundedHeight && constraints.maxHeight > 0) {
            constraints.maxWidth.toFloat() / constraints.maxHeight
        } else {
            StackedScreenshot.DEFAULT_SLOT_ASPECT
        }
        val aspectKey = (slotAspect * ASPECT_KEY_PRECISION).roundToInt()
        val request = remember(model, aspectKey) { screenshotRequest(context, model, slotAspect) }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            onError = onError,
            contentScale = if (slotAspect > 1f) ContentScale.Crop else ContentScale.Fit
        )
    }
}

private const val ASPECT_KEY_PRECISION = 100f

/**
 * Crops a stacked dual-screen screenshot to its top screen for a landscape backdrop. Any other
 * image passes through unchanged. Never call it for a cover: a cover can share a stacked frame's
 * shape.
 */
fun ImageRequest.Builder.stackedScreenshotBackdrop(): ImageRequest.Builder =
    transformations(stackedScreenshotTransformation(StackedScreenshot.DEFAULT_SLOT_ASPECT))

private fun screenshotRequest(context: Context, model: Any?, slotAspect: Float): ImageRequest {
    val transformation = stackedScreenshotTransformation(slotAspect)
    val source = model as? ImageRequest
    val builder = source?.newBuilder() ?: ImageRequest.Builder(context).data(model)
    if (transformation != null) {
        source?.memoryCacheKey?.let { key -> builder.memoryCacheKey("$key#${stackedScreenshotCacheKey(slotAspect)}") }
    }
    return builder.transformations(transformation).build()
}

internal fun stackedScreenshotCacheKey(slotAspect: Float): String =
    "stacked-screenshot:${(slotAspect * ASPECT_KEY_PRECISION).roundToInt()}"

/**
 * The image transformation that crops a stacked dual-screen frame for a slot of [slotAspect];
 * null where the platform's image loader has no transformation stage.
 */
expect fun stackedScreenshotTransformation(slotAspect: Float): Any?

internal object StackedScreenshot {

    const val DEFAULT_SLOT_ASPECT = 16f / 9f
    private const val DS_ASPECT = 256f / 384f
    private const val N3DS_ASPECT = 400f / 480f
    private const val ASPECT_TOLERANCE = 0.02f

    fun isStacked(width: Float, height: Float): Boolean {
        if (width <= 0f || height <= width) return false
        val aspect = width / height
        return abs(aspect - DS_ASPECT) <= ASPECT_TOLERANCE || abs(aspect - N3DS_ASPECT) <= ASPECT_TOLERANCE
    }

    fun topScreenHeight(height: Float): Float = height / 2f

    fun sourceRect(width: Int, height: Int, slotAspect: Float): IntRect? {
        if (width <= 0 || height <= 0) return null
        val stacked = isStacked(width.toFloat(), height.toFloat())
        val landscape = slotAspect > 1f
        return when {
            stacked && landscape -> IntRect(0, 0, width, topScreenHeight(height.toFloat()).roundToInt())
            stacked || landscape -> null
            else -> centerCrop(width, height, slotAspect)
        }
    }

    private fun centerCrop(width: Int, height: Int, slotAspect: Float): IntRect? {
        val imageAspect = width.toFloat() / height
        return when {
            imageAspect > slotAspect -> {
                val cropWidth = (height * slotAspect).roundToInt().coerceIn(1, width)
                val left = (width - cropWidth) / 2
                IntRect(left, 0, left + cropWidth, height)
            }
            imageAspect < slotAspect -> {
                val cropHeight = (width / slotAspect).roundToInt().coerceIn(1, height)
                val top = (height - cropHeight) / 2
                IntRect(0, top, width, top + cropHeight)
            }
            else -> null
        }
    }
}
