package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.AppEventFeedback
import com.nendo.argosy.core.input.SoundConfig
import com.nendo.argosy.core.input.SoundType

expect class SoundFeedbackManager : AppEventFeedback, com.nendo.argosy.core.media.SfxCacheCleaner {
    fun playSound(type: SoundType)

    override fun play(type: SoundType)

    fun setEnabled(enabled: Boolean)

    override suspend fun clearSfxCache()

    fun setVolume(volume: Int)

    fun setSoundConfig(type: SoundType, config: SoundConfig)

    fun clearSoundConfig(type: SoundType)

    fun setSoundConfigs(configs: Map<SoundType, SoundConfig>)
}
