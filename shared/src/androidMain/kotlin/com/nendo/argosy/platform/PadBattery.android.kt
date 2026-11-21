package com.nendo.argosy.platform

import android.hardware.BatteryState
import android.os.Build

actual fun PlatformInputDevice.padBattery(): PadBatteryReading? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val battery = batteryState
    if (!battery.isPresent || battery.capacity.isNaN()) return null
    return PadBatteryReading(battery.capacity, battery.status == BatteryState.STATUS_CHARGING)
}
