package com.nendo.argosy.ui.common

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
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
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.data.download.DownloadState
import com.nendo.argosy.ui.screens.home.GameDownloadIndicator

fun DownloadState.toIndicator(progressPercent: Float, extractionPercent: Float): GameDownloadIndicator =
    when (this) {
        DownloadState.DOWNLOADING -> GameDownloadIndicator(isDownloading = true, progress = progressPercent)
        DownloadState.EXTRACTING,
        DownloadState.MOVING -> GameDownloadIndicator(isExtracting = true, progress = extractionPercent)
        DownloadState.PAUSED -> GameDownloadIndicator(isPaused = true, progress = progressPercent)
        DownloadState.QUEUED -> GameDownloadIndicator(isQueued = true)
        else -> GameDownloadIndicator.NONE
    }
