package com.nendo.argosy.hardware

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class HardwareStatsReader @Inject constructor() {
    actual fun read(): HardwareSample = HardwareSample(
        cpuKhz = null,
        cpuMaxKhz = null,
        gpuHz = null,
        gpuMaxHz = null,
        gpuBusyPercent = null,
        cpuTempC = null,
        gpuTempC = null,
        fanRpm = null,
        fanFraction = null,
        batteryTempC = null,
        batteryLevel = null
    )
}
