package com.nendo.argosy.data.cache

import java.security.MessageDigest

internal fun artUrlHash(url: String): String {
    val digest = MessageDigest.getInstance("MD5").digest(url.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }.take(12)
}
