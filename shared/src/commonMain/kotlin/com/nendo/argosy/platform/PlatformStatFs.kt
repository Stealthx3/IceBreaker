package com.nendo.argosy.platform

expect class PlatformStatFs(path: String) {
    val availableBytes: Long
    val blockSizeLong: Long
    val blockCountLong: Long
    val availableBlocksLong: Long
}
