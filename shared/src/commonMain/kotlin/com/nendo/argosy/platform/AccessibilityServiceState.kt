package com.nendo.argosy.platform

expect object AccessibilityServiceState {
    fun isEnabled(context: PlatformContext): Boolean
}
