package com.nendo.argosy.hardware


expect class HardwareStatsReader {
    fun read(): HardwareSample
}
