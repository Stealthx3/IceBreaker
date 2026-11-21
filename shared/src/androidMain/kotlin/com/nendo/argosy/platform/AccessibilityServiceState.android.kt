package com.nendo.argosy.platform

import com.nendo.argosy.hardware.FocusAccessibilityService

actual object AccessibilityServiceState {
    actual fun isEnabled(context: PlatformContext): Boolean = FocusAccessibilityService.isEnabled(context)
}
