package com.nendo.argosy.platform

import com.nendo.argosy.data.social.SocialOverlayService

actual object TransientNotice {
    actual fun show(context: PlatformContext, title: String, subtitle: String?) =
        SocialOverlayService.show(context, title, subtitle)
}
