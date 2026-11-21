package com.nendo.argosy.data.scanner

import kotlinx.coroutines.flow.Flow

interface RomScanActivity {
    val isScanning: Flow<Boolean>
}
