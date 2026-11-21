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
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.core.locale.LocaleHelper
import com.nendo.argosy.data.preferences.SessionStateStore
import com.nendo.argosy.platform.TransientNotice
import com.nendo.argosy.util.Logger
import com.nendo.argosy.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenshotUploadNotices @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun drainFinished(uploaded: Int, rejected: Int) {
        if (uploaded <= 0 && rejected <= 0) return
        try {
            val localized = LocaleHelper.wrap(context, SessionStateStore(context).getAppLanguage())
            val res = localized.resources
            val uploadedLine = res.getQuantityString(R.plurals.sync_screenshot_uploaded, uploaded, uploaded)
            val rejectedLine = res.getQuantityString(R.plurals.sync_screenshot_rejected, rejected, rejected)
            when {
                uploaded > 0 && rejected > 0 -> TransientNotice.show(context, uploadedLine, rejectedLine)
                uploaded > 0 -> TransientNotice.show(context, uploadedLine)
                else -> TransientNotice.show(context, rejectedLine)
            }
        } catch (e: Exception) {
            Logger.warn(TAG, "Could not show the screenshot upload notice", e)
        }
    }

    private companion object {
        const val TAG = "ScreenshotUploadNotices"
    }
}
