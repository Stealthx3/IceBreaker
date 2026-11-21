package com.nendo.argosy.util

import com.nendo.argosy.platform.PlatformBuild as Build

object AynThor {
    fun matches(model: String = Build.MODEL): Boolean = model.contains("thor", ignoreCase = true)
}
