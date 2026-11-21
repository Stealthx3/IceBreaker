package com.nendo.argosy.hardware

data class RadioStatus(
    val wifiEnabled: Boolean = false,
    val wifiConnected: Boolean = false,
    val bluetoothEnabled: Boolean = false
)

enum class BluetoothDeviceKind { CONTROLLER, AUDIO, OTHER }

data class PairedBluetoothDevice(
    val address: String,
    val name: String,
    val connected: Boolean,
    val kind: BluetoothDeviceKind,
    val batteryPercent: Int?
)
