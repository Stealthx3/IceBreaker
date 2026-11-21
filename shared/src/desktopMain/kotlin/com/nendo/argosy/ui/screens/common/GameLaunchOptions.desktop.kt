package com.nendo.argosy.ui.screens.common

import com.nendo.argosy.platform.PlatformBundle

actual object GameLaunchOptions {
    actual fun solidSplashOn(sdkInt: Int): Boolean = false

    actual fun forDisplay(displayId: Int?, sdkInt: Int): PlatformBundle? = null
}
