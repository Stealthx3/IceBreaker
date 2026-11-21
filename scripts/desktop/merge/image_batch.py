import re
from pathlib import Path

C = Path("shared/src/commonMain/kotlin/com/nendo/argosy")
A = Path("shared/src/androidMain/kotlin/com/nendo/argosy")
D = Path("shared/src/desktopMain/kotlin/com/nendo/argosy")


def edit(path, pairs, regex=False):
    p = Path(path)
    t = p.read_text(encoding="utf-8")
    for a, b in pairs:
        if regex:
            t, n = re.subn(a, b, t, count=1, flags=re.M | re.S)
            assert n == 1, (str(path), a[:80])
        else:
            assert a in t, (str(path), a[:80])
            t = t.replace(a, b, 1)
    p.write_text(t, encoding="utf-8")


def write(path, text):
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text.lstrip("\n"), encoding="utf-8")


IMG = C / "platform/image"
edit(IMG / "ImageRequest.kt", [
    ("    val data: Any? get() = calls.filterIsInstance<BuilderCall.Data>().lastOrNull()?.data\n",
     "    val data: Any? get() = calls.filterIsInstance<BuilderCall.Data>().lastOrNull()?.data\n"
     "    val memoryCacheKey: String? get() = calls.filterIsInstance<BuilderCall.MemoryCacheKey>().lastOrNull()?.key\n\n"
     "    fun newBuilder(): Builder = Builder(context).also { it.replay(calls) }\n"),
    ("        fun addHeader(name: String, value: String): Builder = apply { calls += BuilderCall.AddHeader(name, value) }\n",
     "        fun addHeader(name: String, value: String): Builder = apply { calls += BuilderCall.AddHeader(name, value) }\n"
     "        fun placeholderMemoryCacheKey(key: Any?): Builder = apply { calls += BuilderCall.PlaceholderMemoryCacheKey(key) }\n"
     "        fun transformations(vararg transformations: Any?): Builder =\n"
     "            apply { calls += BuilderCall.Transformations(transformations.filterNotNull()) }\n"
     "        internal fun replay(previous: List<BuilderCall>) {\n"
     "            calls += previous\n"
     "        }\n"),
    ("    data class AddHeader(val name: String, val value: String) : BuilderCall\n",
     "    data class AddHeader(val name: String, val value: String) : BuilderCall\n"
     "    data class PlaceholderMemoryCacheKey(val key: Any?) : BuilderCall\n"
     "    data class Transformations(val transformations: List<Any>) : BuilderCall\n"),
])
edit(A / "platform/image/ImageRequest.android.kt", [
    ("import coil.transition.Transition\n", "import coil.memory.MemoryCache\nimport coil.transform.Transformation\nimport coil.transition.Transition\n"),
    ("            is BuilderCall.AddHeader -> builder.addHeader(call.name, call.value)\n",
     "            is BuilderCall.AddHeader -> builder.addHeader(call.name, call.value)\n"
     "            is BuilderCall.PlaceholderMemoryCacheKey -> builder.placeholderMemoryCacheKey(call.key as MemoryCache.Key?)\n"
     "            is BuilderCall.Transformations -> builder.transformations(call.transformations.map { it as Transformation })\n"),
])
edit(D / "platform/image/ImageRequest.desktop.kt", [
    (r"(            is BuilderCall\.AllowHardware -> Unit\n)", r"\1            is BuilderCall.PlaceholderMemoryCacheKey -> Unit\n            is BuilderCall.Transformations -> Unit\n"),
], regex=True)

edit(IMG / "PlatformDrawable.kt", [("class AsyncImageSuccessResult(val drawable: PlatformDrawable)",
                                    "class AsyncImageSuccessResult(val drawable: PlatformDrawable, val memoryCacheKey: Any? = null)")])
edit(A / "platform/image/AsyncImage.android.kt", [("callback(AsyncImageSuccess(AsyncImageSuccessResult(state.result.drawable)))",
                                                   "callback(AsyncImageSuccess(AsyncImageSuccessResult(state.result.drawable, state.result.memoryCacheKey)))")])

# ScreenshotImage: the stacked-screen crop is a Coil transformation on Android
S = C / "ui/common/ScreenshotImage.kt"
t = S.read_text(encoding="utf-8")
for imp in ["import coil.compose.AsyncImagePainter\n", "import coil.memory.MemoryCache\n", "import coil.size.Size\n", "import coil.transform.Transformation\n"]:
    assert imp in t, imp
    t = t.replace(imp, "")
t = t.replace("    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null\n", "    onError: ((Any) -> Unit)? = null\n")
old_req = t[t.index("private fun screenshotRequest("):t.index("internal object StackedScreenshot")]
t = t.replace(old_req, '''private fun screenshotRequest(context: Context, model: Any?, slotAspect: Float): ImageRequest {
    val transformation = stackedScreenshotTransformation(slotAspect)
    val source = model as? ImageRequest
    val builder = source?.newBuilder() ?: ImageRequest.Builder(context).data(model)
    if (transformation != null) {
        source?.memoryCacheKey?.let { key -> builder.memoryCacheKey("$key#${stackedScreenshotCacheKey(slotAspect)}") }
    }
    return builder.transformations(transformation).build()
}

internal fun stackedScreenshotCacheKey(slotAspect: Float): String =
    "stacked-screenshot:${(slotAspect * ASPECT_KEY_PRECISION).roundToInt()}"

/**
 * The image transformation that crops a stacked dual-screen frame for a slot of [slotAspect];
 * null where the platform's image loader has no transformation stage.
 */
expect fun stackedScreenshotTransformation(slotAspect: Float): Any?

''')
t = t.replace("    transformations(StackedScreenshotTransformation(StackedScreenshot.DEFAULT_SLOT_ASPECT))",
              "    transformations(stackedScreenshotTransformation(StackedScreenshot.DEFAULT_SLOT_ASPECT))")
S.write_text(t, encoding="utf-8")
write(A / "ui/common/StackedScreenshotTransformation.kt", """
package com.nendo.argosy.ui.common

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

actual fun stackedScreenshotTransformation(slotAspect: Float): Any? = StackedScreenshotTransformation(slotAspect)

private class StackedScreenshotTransformation(private val slotAspect: Float) : Transformation {

    override val cacheKey: String = stackedScreenshotCacheKey(slotAspect)

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val rect = StackedScreenshot.sourceRect(input.width, input.height, slotAspect) ?: return input
        return Bitmap.createBitmap(input, rect.left, rect.top, rect.width, rect.height)
    }
}
""")
write(D / "ui/common/StackedScreenshotTransformation.desktop.kt", """
package com.nendo.argosy.ui.common

actual fun stackedScreenshotTransformation(slotAspect: Float): Any? = null
""")

# Showcase backdrop: Coil types go through the wrapper
M = C / "ui/dualscreen/ShowcaseMotionLayers.kt"
t = M.read_text(encoding="utf-8")
for imp in ["import coil.compose.AsyncImagePainter\n", "import coil.memory.MemoryCache\n", "import coil.transition.Transition\n"]:
    assert imp in t, imp
    t = t.replace(imp, "")
for a, b in [
    ("    val onSuccess: (AsyncImagePainter.State.Success) -> Unit\n", "    val onSuccess: (com.nendo.argosy.platform.image.AsyncImageSuccess) -> Unit\n"),
    ("val lastKey = remember { mutableStateOf<MemoryCache.Key?>(null) }", "val lastKey = remember { mutableStateOf<Any?>(null) }"),
    ("            .transitionFactory(if (crossfadeMs > 0) AlwaysCrossfadeFactory(crossfadeMs) else Transition.Factory.NONE)\n",
     "            .apply { if (crossfadeMs > 0) transitionFactory(AlwaysCrossfadeFactory(crossfadeMs)) else crossfade(false) }\n"),
]:
    assert a in t, a
    t = t.replace(a, b)
M.write_text(t, encoding="utf-8")
print("image batch done")
