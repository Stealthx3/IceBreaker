package com.nendo.argosy.data.emulator

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nendo.argosy.util.Logger

private const val TAG = "LaunchCommand"

/**
 * The shared-storage folder holding the file [uri] names, as a URI of the same provider. Null for
 * any folder [sharedFolderSegments] refuses.
 */
fun sharedFolderUriOf(uri: Uri): Uri? {
    val folder = sharedFolderSegments(uri.pathSegments) ?: return null
    return uri.buildUpon().path(null).apply { folder.forEach(::appendPath) }.build()
}

/**
 * The folder segments holding the file [fileSegments] names under the FileProvider's `storage` or
 * `mnt` root. Null for a file under one of Argosy's private roots, for a folder that is a storage
 * volume itself, and for a folder at or under a volume's `Android` directory, where app-specific
 * storage lives.
 */
fun sharedFolderSegments(fileSegments: List<String>): List<String>? {
    val folder = fileSegments.dropLast(1)
    val volumeDepth = when {
        folder.size >= 2 && folder[0] == "storage" && folder[1] in setOf("emulated", "self") -> 3
        folder.size >= 2 && folder[0] == "mnt" && folder[1] == "media_rw" -> 3
        folder.firstOrNull() == "storage" -> 2
        else -> return null
    }
    if (folder.size <= volumeDepth) return null
    if (folder[volumeDepth].equals("Android", ignoreCase = true)) return null
    return folder
}

/**
 * Grants [packageName] read access to every own-FileProvider URI in [EffectiveLaunchCommand.grantReadUriTo],
 * and to everything under the shared-storage folder holding it, so an emulator can open the tracks
 * a cue, gdi or m3u lists beside it.
 */
fun EffectiveLaunchCommand.grantLaunchReads(context: Context) {
    val ownFileProviderAuthority = "${context.packageName}.fileprovider"
    for (uri in grantReadUriTo) {
        if (uri.scheme != "content") {
            Logger.warn(TAG, "Skipping URI grant for non-content scheme: $uri")
            continue
        }
        if (uri.authority != ownFileProviderAuthority) {
            Logger.warn(TAG, "Refusing URI grant to $packageName for foreign authority '${uri.authority}' - only $ownFileProviderAuthority can be granted")
            continue
        }
        try {
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            sharedFolderUriOf(uri)?.let { folder ->
                context.grantUriPermission(
                    packageName,
                    folder,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                )
            }
        } catch (e: Exception) {
            Logger.warn(TAG, "Failed to grant URI permission to $packageName for $uri", e)
        }
    }
}

actual fun EffectiveLaunchCommand.toIntent(context: Context): Intent {
    grantLaunchReads(context)

    val cmdCategories = categories
    val cmdExtras = extras
    val cmdClipDataUri = clipDataUri
    val cmdFlags = intentFlags

    val intent = Intent(action)
    if (activityClass != null) {
        intent.component = ComponentName(packageName, activityClass)
    } else {
        intent.setPackage(packageName)
    }
    for (category in cmdCategories) {
        intent.addCategory(category)
    }
    if (dataUri != null) {
        if (mimeType != null) {
            intent.setDataAndType(dataUri, mimeType)
        } else {
            intent.data = dataUri
        }
    }
    for (extra in cmdExtras) {
        when (extra) {
            is ResolvedExtra.StringExtra -> intent.putExtra(extra.key, extra.value)
            is ResolvedExtra.UriExtra -> intent.putExtra(extra.key, extra.uri)
            is ResolvedExtra.BoolExtra -> intent.putExtra(extra.key, extra.value)
            is ResolvedExtra.StringArrayExtra -> intent.putExtra(extra.key, extra.values)
        }
    }
    if (cmdClipDataUri != null) {
        intent.clipData = ClipData.newRawUri(null, cmdClipDataUri)
    }
    if (cmdFlags != 0) {
        intent.addFlags(cmdFlags)
    }

    val hasContentUri = dataUri?.scheme == "content" ||
        cmdExtras.any { it is ResolvedExtra.UriExtra && it.uri.scheme == "content" } ||
        cmdClipDataUri?.scheme == "content" ||
        grantReadUriTo.isNotEmpty()
    if (hasContentUri) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return intent
}

/**
 * This command pinned to the activity [resolve] finds for a dot-free probe path ending in
 * [romExtension], when the package-only intent for the real path resolves to none. [resolve] takes
 * null for the real path or the probe path, and answers only when exactly one activity matches.
 * Unchanged when an activity is already named, the command is not a VIEW of a data URI, or the
 * probe also misses.
 */
fun EffectiveLaunchCommand.pinnedPastPathPattern(
    romExtension: String,
    resolve: (pathOverride: String?) -> String?
): EffectiveLaunchCommand {
    if (activityClass != null || action != Intent.ACTION_VIEW || dataUri == null || romExtension.isEmpty()) {
        return this
    }
    if (resolve(null) != null) return this
    val activity = resolve("/rom.$romExtension") ?: return this
    return copy(activityClass = activity)
}
