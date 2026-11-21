package com.nendo.argosy.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

actual fun PlatformContext.hostIsChangingConfigurations(): Boolean =
    findActivity()?.isChangingConfigurations == true

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
