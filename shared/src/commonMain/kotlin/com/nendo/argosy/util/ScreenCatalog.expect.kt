package com.nendo.argosy.util

expect class ScreenCatalog {
    companion object {
        fun builtInScreenCount(context: com.nendo.argosy.platform.PlatformContext): Int
    }

    fun attachedScreens(): List<AttachedScreen>

    fun screenFor(displayId: Int): AttachedScreen?
}
