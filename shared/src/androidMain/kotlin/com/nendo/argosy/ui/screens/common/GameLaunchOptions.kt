package com.nendo.argosy.ui.screens.common

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.os.Build
import android.os.Bundle
import android.window.SplashScreen

actual object GameLaunchOptions {

    actual fun solidSplashOn(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU

    @SuppressLint("NewApi")
    actual fun forDisplay(displayId: Int?, sdkInt: Int): Bundle? {
        val solidSplash = solidSplashOn(sdkInt)
        if (displayId == null && !solidSplash) return null
        val options = ActivityOptions.makeBasic()
        displayId?.let { options.setLaunchDisplayId(it) }
        if (solidSplash) options.setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_SOLID_COLOR)
        return options.toBundle()
    }
}
