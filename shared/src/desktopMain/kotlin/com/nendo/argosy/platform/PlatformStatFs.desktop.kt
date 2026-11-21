package com.nendo.argosy.platform

import java.io.File

actual class PlatformStatFs actual constructor(path: String) {
    private val file = File(path).also { require(it.exists()) { "Invalid path: $path" } }

    actual val availableBytes: Long get() = file.usableSpace
    actual val blockSizeLong: Long get() = BLOCK_SIZE
    actual val blockCountLong: Long get() = file.totalSpace / BLOCK_SIZE
    actual val availableBlocksLong: Long get() = file.usableSpace / BLOCK_SIZE

    private companion object {
        const val BLOCK_SIZE = 4096L
    }
}
