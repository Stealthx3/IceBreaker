package com.nendo.argosy.ui.common

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

actual fun stackedScreenshotTransformation(slotAspect: Float): Any? = StackedScreenshotTransformation(slotAspect)

private class StackedScreenshotTransformation(private val slotAspect: Float) : Transformation {

    override val cacheKey: String = stackedScreenshotCacheKey(slotAspect)

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val rect = StackedScreenshot.sourceRect(input.width, input.height, slotAspect) ?: return input
        return Bitmap.createBitmap(input, rect.left, rect.top, rect.width, rect.height)
    }
}
