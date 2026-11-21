package com.nendo.argosy.data.sync

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.MODE_PRIVATE
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.platform.PlatformSharedPreferences as SharedPreferences
import com.nendo.argosy.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source screenshots RomM has already answered for, keyed by account, rom, source path and
 * modification time. Entries expire after [RETENTION_MS]; past that the server's own
 * same-name dedupe keeps a repeat upload from adding a second copy.
 */
@Singleton
class ScreenshotUploadLedger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    fun contains(ownerUserId: Long?, rommId: Long, sourcePath: String, sourceModifiedAt: Long): Boolean =
        prefs.contains(keyOf(ownerUserId, rommId, sourcePath, sourceModifiedAt))

    fun record(
        ownerUserId: Long?,
        rommId: Long,
        sourcePath: String,
        sourceModifiedAt: Long,
        now: Long = System.currentTimeMillis()
    ) {
        val cutoff = now - RETENTION_MS
        val editor = prefs.edit()
        prefs.getAll().forEach { (key, value) ->
            if ((value as? Long ?: 0L) < cutoff) editor.remove(key)
        }
        editor.putLong(keyOf(ownerUserId, rommId, sourcePath, sourceModifiedAt), now)
        editor.apply()
    }

    private fun keyOf(ownerUserId: Long?, rommId: Long, sourcePath: String, sourceModifiedAt: Long): String =
        "${ownerUserId ?: NO_OWNER}|$rommId|$sourceModifiedAt|$sourcePath"

    private companion object {
        const val PREFS_NAME = "screenshot_upload_ledger"
        const val NO_OWNER = "none"
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
