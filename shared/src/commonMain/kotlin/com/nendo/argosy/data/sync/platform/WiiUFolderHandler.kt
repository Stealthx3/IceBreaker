package com.nendo.argosy.data.sync.platform

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
import com.nendo.argosy.data.storage.FileAccessLayer
import com.nendo.argosy.data.sync.SaveArchiver
import java.io.File

internal class WiiUFolderHandler(
    context: Context,
    fal: FileAccessLayer,
    saveArchiver: SaveArchiver
) : CategorySplitFolderHandler(context, fal, saveArchiver, platformSlug = "wiiu", categoryDir = Regex("0005[0-9a-fA-F]{4}")) {

    companion object {
        private const val USER_DIR = "user"
        private const val COMMON_DIR = "common"
        private val PERSISTENT_ID = Regex("8[0-9a-fA-F]{7}")

        fun isUserFolderArchive(roots: Set<String>): Boolean =
            roots.isNotEmpty() && roots.all { it == COMMON_DIR || PERSISTENT_ID.matches(it) }
    }

    override fun matchArchive(tempFile: File, saveId: String): ArchiveRootMatch? {
        val roots = saveArchiver.peekRootEntryNames(tempFile)
        return when {
            roots.any { it.equals(lowId(saveId), ignoreCase = true) } -> ArchiveRootMatch.EXACT
            isUserFolderArchive(roots) -> ArchiveRootMatch.UNIDENTIFIED
            else -> super.matchArchive(tempFile, saveId)
        }
    }

    override fun unpackArchive(tempFile: File, targetFolder: File, saveId: String?): Boolean {
        val roots = saveArchiver.peekRootEntryNames(tempFile)
        if (!isUserFolderArchive(roots)) return super.unpackArchive(tempFile, targetFolder, saveId)
        val userFolder = File(targetFolder, USER_DIR)
        return saveArchiver.unzipRootsTo(tempFile, roots.associateWith { File(userFolder, it) })
    }
}
