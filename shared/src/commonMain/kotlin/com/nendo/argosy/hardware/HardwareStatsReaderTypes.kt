package com.nendo.argosy.hardware

/**
 * One reading of the device's clocks, temperatures and fan. Each value is null where the device
 * does not expose it or blocks the read.
 */
data class HardwareSample(
    val cpuKhz: Int?,
    val cpuMaxKhz: Int?,
    val gpuHz: Long?,
    val gpuMaxHz: Long?,
    val gpuBusyPercent: Int?,
    val cpuTempC: Float?,
    val gpuTempC: Float?,
    val fanRpm: Int?,
    val fanFraction: Float?,
    val batteryTempC: Float?,
    val batteryLevel: Int?
)
