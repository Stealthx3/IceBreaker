package com.nendo.argosy.hardware

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class RadioController @Inject constructor() {
    actual val wifi: WifiNetworks = WifiNetworks { null }

    actual val canToggle: Boolean get() = false

    actual fun status(): RadioStatus = RadioStatus()

    actual fun setWifiEnabled(enabled: Boolean): Boolean = false

    actual fun setBluetoothEnabled(enabled: Boolean): Boolean = false

    actual fun hasBluetoothPermission(): Boolean = false

    actual fun pairedDevices(): List<PairedBluetoothDevice> = emptyList()
}
