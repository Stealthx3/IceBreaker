package com.nendo.argosy.platform

class PadBatteryReading(val capacity: Float, val isCharging: Boolean)

expect fun PlatformInputDevice.padBattery(): PadBatteryReading?
