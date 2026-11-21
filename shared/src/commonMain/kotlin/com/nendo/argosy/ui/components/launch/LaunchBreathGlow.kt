package com.nendo.argosy.ui.components.launch

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import com.nendo.argosy.platform.asImageBitmap
import com.nendo.argosy.platform.config
import com.nendo.argosy.platform.copy
import com.nendo.argosy.platform.getPixel
import com.nendo.argosy.platform.height
import com.nendo.argosy.platform.recycle
import com.nendo.argosy.platform.setPixels
import com.nendo.argosy.platform.width
import com.nendo.argosy.platform.PlatformBitmapConfig
import com.nendo.argosy.platform.PlatformBlurMaskFilter as BlurMaskFilter
import com.nendo.argosy.platform.PlatformBlur
import com.nendo.argosy.platform.PlatformSystemClock as SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import com.nendo.argosy.ui.common.GradientColorExtractor
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalMotionTier
import com.nendo.argosy.ui.theme.Motion
import com.nendo.argosy.ui.theme.MotionTier
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Defaults = ComponentDefaults.LaunchOverlay

@Composable
internal fun rememberArtGlowColor(art: Bitmap?): Color? {
    val extractor = remember { GradientColorExtractor() }
    val color = remember(art) { mutableStateOf<Color?>(null) }
    LaunchedEffect(art) {
        if (art == null) return@LaunchedEffect
        val extracted = withContext(Dispatchers.Default) { extractor.warmerArtColor(art) }
        if (extracted != null) color.value = extracted
    }
    return color.value
}

private fun GradientColorExtractor.warmerArtColor(art: Bitmap): Color? {
    val readable = if (art.config == PlatformBitmapConfig.HARDWARE) {
        art.copy(PlatformBitmapConfig.ARGB_8888, false) ?: return null
    } else {
        art
    }
    return try {
        val (primary, secondary) = extractGradientColors(readable)
        warmerOf(primary, secondary)
    } finally {
        if (readable !== art) readable.recycle()
    }
}

@Composable
internal fun Modifier.launchBreathGlow(artColor: Color?, breathKey: Any?): Modifier {
    val reduced = LocalMotionTier.current == MotionTier.Reduced
    val color by animateColorAsState(
        targetValue = artColor ?: MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = Defaults.fadeMs, easing = Motion.argosyEase),
        label = "launchBreathColor"
    )
    val startedAtMs = remember(breathKey) { SystemClock.elapsedRealtime() }
    val elapsedMs = remember(breathKey) { mutableLongStateOf(0L) }
    LaunchedEffect(breathKey, reduced) {
        if (reduced) return@LaunchedEffect
        while (true) {
            withFrameMillis { elapsedMs.longValue = SystemClock.elapsedRealtime() - startedAtMs }
        }
    }
    val spread = Defaults.breathSpreadDp.dp
    val blur = Defaults.breathBlurDp.dp
    val corner = Dimens.radiusLg
    return drawWithCache {
        val spreadPx = spread.toPx()
        val cornerPx = corner.toPx() + spreadPx
        val paint = Paint()
        paint.asFrameworkPaint().maskFilter =
            BlurMaskFilter(blur.toPx(), PlatformBlur.NORMAL)
        onDrawBehind {
            val alpha = breathAlpha(
                elapsedMs = elapsedMs.longValue,
                periodMs = Defaults.breathMs,
                floor = Defaults.breathFloorAlpha,
                peak = Defaults.breathPeakAlpha,
                reduced = reduced
            )
            if (alpha > 0f) {
                paint.color = color.copy(alpha = alpha)
                drawIntoCanvas { canvas ->
                    canvas.drawRoundRect(
                        -spreadPx,
                        -spreadPx,
                        size.width + spreadPx,
                        size.height + spreadPx,
                        cornerPx,
                        cornerPx,
                        paint
                    )
                }
            }
        }
    }
}
