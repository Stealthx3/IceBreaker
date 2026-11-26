package com.nendo.argosy.ui.common

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import com.nendo.argosy.ui.theme.LocalArgosyTheme

@Composable
actual fun BrandLockup(modifier: Modifier) {
    val dark = LocalArgosyTheme.current.isDark
    val density = LocalDensity.current
    val painter = remember(dark, density) {
        val name = if (dark) "brand/about-lockup-on-dark.svg" else "brand/about-lockup-on-light.svg"
        BrandLockupResource::class.java.classLoader.getResourceAsStream(name)?.use { loadSvgPainter(it, density) }
    } ?: return
    Image(painter = painter, contentDescription = null, modifier = modifier)
}

private object BrandLockupResource
