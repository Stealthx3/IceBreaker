package com.nendo.argosy.ui.common

import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BrandLockupTest {
    @Test
    fun bothLockupsLoadAsSvgWithASize() {
        for (name in listOf("brand/about-lockup-on-dark.svg", "brand/about-lockup-on-light.svg")) {
            val stream = javaClass.classLoader.getResourceAsStream(name)
            assertNotNull(stream, name)
            val painter = stream.use { loadSvgPainter(it, Density(1f)) }
            assertTrue(painter.intrinsicSize.width > 0f && painter.intrinsicSize.height > 0f, name)
        }
    }
}
