package com.nendo.argosy.platform

import com.nendo.argosy.util.Logger

actual object TransientNotice {
    actual fun show(context: PlatformContext, title: String, subtitle: String?) {
        Logger.info("TransientNotice", listOfNotNull(title, subtitle).joinToString(" | "))
    }
}
