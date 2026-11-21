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

internal open class CategorySplitFolderHandler(
    context: Context,
    fal: FileAccessLayer,
    saveArchiver: SaveArchiver,
    platformSlug: String,
    private val categoryDir: Regex
) : FolderSaveHandler(context, fal, saveArchiver, platformSlug = platformSlug) {

    companion object {
        private const val ID_HEX_LENGTH = 8
        private val FULL_ID = Regex("[0-9a-fA-F]{16}")

        fun lowId(saveId: String): String =
            saveId.trimEnd('/').substringAfterLast('/').takeLast(ID_HEX_LENGTH).lowercase()

        fun categoryOf(saveId: String): String? =
            saveId.replace("/", "").takeIf { FULL_ID.matches(it) }?.take(ID_HEX_LENGTH)?.lowercase()
    }

    fun categoryBase(basePath: String, saveId: String): String {
        val base = basePath.trimEnd('/')
        val category = categoryOf(saveId) ?: return base
        val lastSegment = base.substringAfterLast('/')
        if (!categoryDir.matches(lastSegment) || lastSegment.equals(category, ignoreCase = true)) return base
        return "${base.substringBeforeLast('/')}/$category"
    }

    override fun folderMatches(folderName: String, saveId: String): Boolean =
        folderName.equals(lowId(saveId), ignoreCase = true)

    override fun constructSavePath(baseDir: String, saveId: String): String? =
        "${categoryBase(baseDir, saveId)}/${lowId(saveId)}"

    override fun findSaveFolderBySaveId(basePath: String, saveId: String): String? =
        super.findSaveFolderBySaveId(categoryBase(basePath, saveId), saveId)

    override fun findAllSaveFoldersBySaveId(basePath: String, saveId: String): List<String> =
        super.findAllSaveFoldersBySaveId(categoryBase(basePath, saveId), saveId)

    override fun matchArchive(tempFile: File, saveId: String): ArchiveRootMatch? =
        if (saveArchiver.peekRootEntryNames(tempFile).any { it.equals(lowId(saveId), ignoreCase = true) }) {
            ArchiveRootMatch.EXACT
        } else {
            super.matchArchive(tempFile, saveId)
        }
}
