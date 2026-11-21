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
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import java.io.File

/**
 * Argosy's private copies of screenshots waiting to upload: one folder per source file under
 * filesDir/pending_screenshots, holding the copy under the source's own file name.
 */
object PendingScreenshotStore {
    private const val DIR_NAME = "pending_screenshots"

    fun root(context: Context): File = File(context.filesDir, DIR_NAME)

    fun slotFor(context: Context, sourcePath: String, sourceModifiedAt: Long): File =
        File(root(context), "${sourceModifiedAt}_${sourcePath.hashCode().toUInt().toString(16)}")

    fun discard(context: Context, copy: File) {
        copy.delete()
        val parent = copy.parentFile ?: return
        if (parent.parentFile?.absolutePath == root(context).absolutePath) parent.delete()
    }
}
