package com.nendo.argosy.ui.components

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.data.platform.PlatformDefinitions

object PlatformIconAssets {
    private const val ASSET_DIR = "platforms"

    private val iconFallbacks = mapOf(
        "fbneo" to "arcade",
        "mame" to "arcade",
        "cps1" to "arcade",
        "cps2" to "arcade",
        "cps3" to "arcade",
        "naomi" to "arcade",
        "atomiswave" to "arcade",
        "model2" to "arcade",
        "model3" to "arcade",
        "daphne" to "arcade",
        "hyperneogeo64" to "arcade",
        "xboxseriesx" to "series-x",
        "atari8bit" to "atari800",
        "cdtv" to "commodore-cdtv",
        "odyssey2" to "odyssey-2",
        "neogeocd" to "neogeoaes",
        "dsi" to "nds",
        "supergrafx" to "tg16",
        "nomad" to "genesis",
        "scd" to "genesis",
        "mac" to "apple"
    )

    private val extensions = listOf("svg", "png")

    @Volatile private var cached: Map<String, String>? = null

    fun resolveAssetUri(context: Context, platformSlug: String): String? {
        if (platformSlug.isBlank()) return null
        val available = available(context)
        if (available.isEmpty()) return null
        for (candidate in PlatformDefinitions.getSlugsForCanonical(platformSlug)) {
            available[candidate]?.let { return "file:///android_asset/$ASSET_DIR/$it" }
        }
        iconFallbacks[PlatformDefinitions.getCanonicalSlug(platformSlug)]?.let { fallback ->
            available[fallback]?.let { return "file:///android_asset/$ASSET_DIR/$it" }
        }
        return null
    }

    private fun available(context: Context): Map<String, String> {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val files = context.assets.list(ASSET_DIR)?.toSet() ?: emptySet()
            val byName = files
                .mapNotNull { file -> file.substringBeforeLast('.', "").takeIf { it.isNotEmpty() } }
                .distinct()
                .mapNotNull { name ->
                    extensions.firstOrNull { "$name.$it" in files }?.let { name to "$name.$it" }
                }
                .toMap()
            cached = byName
            return byName
        }
    }
}
