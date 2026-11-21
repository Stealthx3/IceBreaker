package com.nendo.argosy.ui.screens.common

import com.nendo.argosy.platform.PlatformBuild
import com.nendo.argosy.platform.PlatformBundle

/**
 * The start options every game window opens with: its display when one is resolved, and a solid
 * colour starting window where the platform supports one.
 */
expect object GameLaunchOptions {

    /**
     * Whether a start on [sdkInt] can ask for the solid colour splash. The request is public from
     * API 33; on 31 and 32 only the theme's transparent icon applies.
     */
    fun solidSplashOn(sdkInt: Int): Boolean

    fun forDisplay(displayId: Int?, sdkInt: Int = PlatformBuild.VERSION.SDK_INT): PlatformBundle?
}
