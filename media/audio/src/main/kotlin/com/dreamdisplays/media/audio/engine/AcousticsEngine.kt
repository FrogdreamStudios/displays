package com.dreamdisplays.media.audio.engine

import com.dreamdisplays.api.media.audio.model.AcousticQuality
import com.dreamdisplays.api.media.audio.model.ListenerPose
import com.dreamdisplays.api.media.audio.model.SourceAcousticState
import com.dreamdisplays.api.media.audio.service.AudioAcousticsService
import com.dreamdisplays.api.media.audio.service.AudioDspStage
import kotlinx.atomicfu.atomic
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Default [AudioAcousticsService]: the acoustics DSP itself runs natively, so this only holds each registered
 * source's latest published state and forwards the shared listener pose / quality tier / binaural toggle to the
 * native engine.
 */
class AcousticsEngine(
    private val onListenerChanged: (ListenerPose) -> Unit = {},
    private val onQualityChanged: (AcousticQuality) -> Unit = {},
    private val onBinauralChanged: (Boolean) -> Unit = {},
) : AudioAcousticsService {
    private class SourceStateHolder : AudioDspStage {
        @Volatile
        private var state: SourceAcousticState? = null

        fun updateState(newState: SourceAcousticState) {
            state = newState
        }

        override fun process(buf: ByteArray, len: Int, legacyGain: Double) {}
        override fun reset() {}
        override fun latestState(): SourceAcousticState? = state
    }

    private val sources = ConcurrentHashMap<UUID, SourceStateHolder>()
    private val listenerRef = atomic(ListenerPose.IDENTITY)
    private val qualityRef = atomic(AcousticQuality.ADVANCED)
    private val binauralRef = atomic(true)

    fun setBinauralOutput(binaural: Boolean) {
        binauralRef.value = binaural
        onBinauralChanged(binaural)
    }

    override fun registerSource(id: UUID): AudioDspStage =
        sources.computeIfAbsent(id) { SourceStateHolder() }

    override fun unregisterSource(id: UUID) {
        sources.remove(id)?.close()
    }

    override fun updateSource(id: UUID, state: SourceAcousticState) {
        sources[id]?.updateState(state)
    }

    override fun updateListener(pose: ListenerPose) {
        listenerRef.value = pose
        onListenerChanged(pose)
    }

    override fun setGlobalQuality(quality: AcousticQuality) {
        qualityRef.value = quality
        onQualityChanged(quality)
    }
}
