package com.nendo.argosy.platform

import android.os.StatFs

actual class PlatformStatFs actual constructor(path: String) {
    private val stat = StatFs(path)

    actual val availableBytes: Long get() = stat.availableBytes
    actual val blockSizeLong: Long get() = stat.blockSizeLong
    actual val blockCountLong: Long get() = stat.blockCountLong
    actual val availableBlocksLong: Long get() = stat.availableBlocksLong
}
