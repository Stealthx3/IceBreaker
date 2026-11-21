package com.nendo.argosy.hardware

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class LidSleepGuard @Inject constructor() {
    actual val isSupported: Boolean get() = false

    actual fun start() = Unit
}
