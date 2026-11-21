package com.nendo.argosy.platform

expect object TransientNotice {
    fun show(context: PlatformContext, title: String, subtitle: String? = null)
}
