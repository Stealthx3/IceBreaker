package com.nendo.argosy.hardware


expect class RadioController {
    val wifi: WifiNetworks

    val canToggle: Boolean

    fun status(): RadioStatus

    fun setWifiEnabled(enabled: Boolean): Boolean

    fun setBluetoothEnabled(enabled: Boolean): Boolean

    fun hasBluetoothPermission(): Boolean

    fun pairedDevices(): List<PairedBluetoothDevice>
}
