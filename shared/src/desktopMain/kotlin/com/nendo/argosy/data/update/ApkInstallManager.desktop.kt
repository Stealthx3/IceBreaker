package com.nendo.argosy.data.update

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow

@Singleton
actual class ApkInstallManager @Inject constructor() {
    actual val state: StateFlow<ApkInstallState> = kotlinx.coroutines.flow.MutableStateFlow<ApkInstallState>(ApkInstallState.Idle)

    actual fun canInstallPackages(): Boolean = false

    actual fun openInstallPermissionSettings() {
    }

    actual fun resetState() {
    }

    actual suspend fun installApkForGame(gameId: Long): Boolean = false

    actual val pushedPrompt: kotlinx.coroutines.flow.StateFlow<com.nendo.argosy.data.model.PushedApkPrompt?> =
        kotlinx.coroutines.flow.MutableStateFlow(null)

    actual fun confirmPushedInstall() = Unit

    actual fun dismissPushedInstall() = Unit
}
