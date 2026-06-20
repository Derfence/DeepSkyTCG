package fr.aumombelli.dstcg.audio

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class AmbientPlaybackController(
    private val assets: AssetManager,
    private val audioAttributes: AudioAttributes,
    private val scope: CoroutineScope,
) {
    private var activeSlot: AmbientSlot? = null
    private var retiringSlot: AmbientSlot? = null
    private var transitionJob: Job? = null
    private var duckJob: Job? = null
    private var duckGain = 1f
    private var released = false

    fun update(
        track: AmbientTrack?,
        enabled: Boolean,
        foregrounded: Boolean,
    ) {
        if (released) return
        if (!enabled || !foregrounded || track == null) {
            fadeOutAndPause(track?.mix?.fadeOutMillis ?: DefaultAmbientFadeOutMillis)
            return
        }

        val currentSlot = activeSlot
        if (currentSlot?.track == track) {
            currentSlot.start()
            fadeSlotTo(currentSlot, targetFadeGain = 1f, durationMillis = track.mix.fadeInMillis)
        } else {
            crossFadeTo(track)
        }
    }

    fun duck(ducking: AmbientDucking) {
        if (released || activeSlot == null) return
        duckJob?.cancel()
        duckJob = scope.launch {
            animateDuckGain(
                targetGain = ducking.targetGain,
                durationMillis = ducking.attackMillis,
            )
            delay(ducking.holdMillis.toLong())
            animateDuckGain(
                targetGain = 1f,
                durationMillis = ducking.releaseMillis,
            )
        }
    }

    fun release() {
        if (released) return
        released = true
        transitionJob?.cancel()
        duckJob?.cancel()
        activeSlot?.release()
        retiringSlot?.release()
        activeSlot = null
        retiringSlot = null
    }

    private fun crossFadeTo(track: AmbientTrack) {
        transitionJob?.cancel()
        val previousSlot = activeSlot
        val nextSlot = createSlot(track, fadeGain = 0f)
        activeSlot = nextSlot
        retiringSlot = previousSlot
        nextSlot.start()

        transitionJob = scope.launch {
            coroutineScope {
                launch {
                    animateSlotFade(
                        slot = nextSlot,
                        targetFadeGain = 1f,
                        durationMillis = track.mix.crossFadeMillis,
                    )
                }
                previousSlot?.let { slot ->
                    launch {
                        animateSlotFade(
                            slot = slot,
                            targetFadeGain = 0f,
                            durationMillis = slot.track.mix.crossFadeMillis,
                        )
                        slot.release()
                        if (retiringSlot === slot) {
                            retiringSlot = null
                        }
                    }
                }
            }
        }
    }

    private fun fadeOutAndPause(durationMillis: Int) {
        val slot = activeSlot
        val retiring = retiringSlot
        if (slot == null && retiring == null) return
        transitionJob?.cancel()
        transitionJob = scope.launch {
            coroutineScope {
                slot?.let { active ->
                    launch {
                        animateSlotFade(
                            slot = active,
                            targetFadeGain = 0f,
                            durationMillis = durationMillis,
                        )
                        active.pause()
                    }
                }
                retiring?.let { old ->
                    launch {
                        animateSlotFade(
                            slot = old,
                            targetFadeGain = 0f,
                            durationMillis = durationMillis,
                        )
                        old.release()
                        if (retiringSlot === old) {
                            retiringSlot = null
                        }
                    }
                }
            }
        }
    }

    private fun fadeSlotTo(
        slot: AmbientSlot,
        targetFadeGain: Float,
        durationMillis: Int,
    ) {
        val retiring = retiringSlot
        transitionJob?.cancel()
        transitionJob = scope.launch {
            coroutineScope {
                launch {
                    animateSlotFade(
                        slot = slot,
                        targetFadeGain = targetFadeGain,
                        durationMillis = durationMillis,
                    )
                }
                retiring?.let { old ->
                    launch {
                        animateSlotFade(
                            slot = old,
                            targetFadeGain = 0f,
                            durationMillis = old.track.mix.crossFadeMillis,
                        )
                        old.release()
                        if (retiringSlot === old) {
                            retiringSlot = null
                        }
                    }
                }
            }
        }
    }

    private fun createSlot(
        track: AmbientTrack,
        fadeGain: Float,
    ): AmbientSlot {
        val player = MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
            assets.openFd(track.assetPath).use { asset ->
                setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
            }
            isLooping = true
            prepare()
        }
        return AmbientSlot(
            track = track,
            player = player,
            fadeGain = fadeGain,
        ).also(::applySlotVolume)
    }

    private suspend fun animateSlotFade(
        slot: AmbientSlot,
        targetFadeGain: Float,
        durationMillis: Int,
    ) {
        val clampedTarget = targetFadeGain.coerceIn(0f, 1f)
        val start = slot.fadeGain
        if (durationMillis <= 0) {
            slot.fadeGain = clampedTarget
            applySlotVolume(slot)
            return
        }

        val steps = (durationMillis / AmbientFadeFrameMillis).coerceAtLeast(1)
        val frameDelay = (durationMillis / steps).coerceAtLeast(1)
        repeat(steps) { index ->
            val progress = (index + 1).toFloat() / steps.toFloat()
            slot.fadeGain = lerp(start, clampedTarget, progress)
            applySlotVolume(slot)
            delay(frameDelay.toLong())
        }
    }

    private suspend fun animateDuckGain(
        targetGain: Float,
        durationMillis: Int,
    ) {
        val clampedTarget = targetGain.coerceIn(0f, 1f)
        val start = duckGain
        if (durationMillis <= 0) {
            duckGain = clampedTarget
            applyAllSlotVolumes()
            return
        }

        val steps = (durationMillis / AmbientFadeFrameMillis).coerceAtLeast(1)
        val frameDelay = (durationMillis / steps).coerceAtLeast(1)
        repeat(steps) { index ->
            val progress = (index + 1).toFloat() / steps.toFloat()
            duckGain = lerp(start, clampedTarget, progress)
            applyAllSlotVolumes()
            delay(frameDelay.toLong())
        }
    }

    private fun applyAllSlotVolumes() {
        activeSlot?.let(::applySlotVolume)
        retiringSlot?.let(::applySlotVolume)
    }

    private fun applySlotVolume(slot: AmbientSlot) {
        val volume = (slot.track.mix.volume * slot.fadeGain * duckGain).coerceIn(0f, 1f)
        runCatching {
            slot.player.setVolume(volume, volume)
        }
    }

    private data class AmbientSlot(
        val track: AmbientTrack,
        val player: MediaPlayer,
        var fadeGain: Float,
    ) {
        fun start() {
            runCatching {
                if (!player.isPlaying) {
                    player.start()
                }
            }
        }

        fun pause() {
            runCatching {
                if (player.isPlaying) {
                    player.pause()
                }
            }
        }

        fun release() {
            runCatching {
                player.release()
            }
        }
    }

    private companion object {
        const val AmbientFadeFrameMillis = 16
    }
}

private fun lerp(
    start: Float,
    end: Float,
    progress: Float,
): Float = start + (end - start) * progress
