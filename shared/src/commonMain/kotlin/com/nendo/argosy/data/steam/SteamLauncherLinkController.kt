package com.nendo.argosy.data.steam

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
import com.nendo.argosy.core.notification.NotificationManager
import com.nendo.argosy.core.notification.NotificationText
import com.nendo.argosy.core.notification.NotificationType
import com.nendo.argosy.data.launcher.SteamLaunchers
import com.nendo.argosy.data.local.dao.GameArtDao
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.resolved
import com.nendo.argosy.data.model.SteamLauncherLinkPrompt
import com.nendo.argosy.data.model.SteamMarkOption
import com.nendo.argosy.di.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SteamLauncherLinkController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gameDao: GameDao,
    private val gameArtDao: GameArtDao,
    private val notificationManager: NotificationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _prompt = MutableStateFlow<SteamLauncherLinkPrompt?>(null)
    val prompt: StateFlow<SteamLauncherLinkPrompt?> = _prompt.asStateFlow()

    private val _markOptions = MutableStateFlow<List<SteamMarkOption>>(emptyList())
    val markOptions: StateFlow<List<SteamMarkOption>> = _markOptions.asStateFlow()

    private val _focusIndex = MutableStateFlow(0)
    val focusIndex: StateFlow<Int> = _focusIndex.asStateFlow()

    fun requestLink(gameId: Long) {
        scope.launch {
            val game = gameDao.getById(gameId) ?: return@launch
            if (game.steamAppId == null) return@launch
            val options = SteamLaunchers.getMarkOptions(context).map {
                SteamMarkOption(it.launcherPackage, it.displayName)
            }
            if (options.isEmpty()) {
                notificationManager.show(
                    title = NotificationText.Res(R.string.notif_steam_link_no_launcher_installed),
                    type = NotificationType.ERROR
                )
                return@launch
            }
            _markOptions.value = options
            _focusIndex.value = 0
            _prompt.value = SteamLauncherLinkPrompt(
                gameId = gameId,
                title = game.title,
                coverPath = gameArtDao.resolved(gameId).coverPath
            )
        }
    }

    fun moveFocus(delta: Int) {
        val size = _markOptions.value.size
        if (size == 0) return
        _focusIndex.update { (it + delta).mod(size) }
    }

    fun confirmFocused() {
        val option = _markOptions.value.getOrNull(_focusIndex.value) ?: return
        confirmMarkInstalled(option.launcherPackage)
    }

    fun confirmMarkInstalled(launcherPackage: String) {
        val p = _prompt.value ?: return
        scope.launch {
            gameDao.setSteamLauncher(p.gameId, launcherPackage)
            clearPrompt()
        }
    }

    fun dismiss() {
        clearPrompt()
    }

    fun unlinkLauncher(gameId: Long) {
        scope.launch { gameDao.setSteamLauncher(gameId, null) }
    }

    private fun clearPrompt() {
        _prompt.value = null
        _markOptions.value = emptyList()
        _focusIndex.value = 0
    }
}
