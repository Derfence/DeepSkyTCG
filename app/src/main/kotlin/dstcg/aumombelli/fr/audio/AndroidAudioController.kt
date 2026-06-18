package fr.aumombelli.dstcg.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AndroidAudioController(
    context: Context,
    private val settingsRepository: AudioSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : AudioController {
    private val appContext = context.applicationContext
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val ambientAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(AudioMaxStreams)
        .setAudioAttributes(audioAttributes)
        .build()
    private val loadedSoundIds = mutableSetOf<Int>()
    private val soundIds = mutableMapOf<SoundCue, Int>()
    private val lastPlayedAtMillis = mutableMapOf<SoundCue, Long>()
    private val ambientPlayback = AmbientPlaybackController(
        assets = appContext.assets,
        audioAttributes = ambientAudioAttributes,
        scope = scope,
    )
    private var currentAmbientTrack: AmbientTrack? = null
    private var appForegrounded = true
    private var released = false

    override val settings: StateFlow<AudioSettings> = settingsRepository.settings.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = AudioSettings(),
    )

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds += sampleId
            }
        }
        val soundIdsByAssetPath = SoundCue.entries
            .map { it.assetPath }
            .distinct()
            .associateWith { assetPath ->
                appContext.assets.openFd(assetPath).use { asset ->
                    soundPool.load(asset, 1)
                }
            }
        SoundCue.entries.associateWithTo(soundIds) { cue ->
            checkNotNull(soundIdsByAssetPath[cue.assetPath]) {
                "Missing loaded sound id for ${cue.assetPath}"
            }
        }
        scope.launch {
            settings.collect {
                applyAmbientState()
            }
        }
    }

    override fun play(cue: SoundCue) {
        if (released || !settings.value.enabled) return
        val mix = cue.mix
        val now = SystemClock.elapsedRealtime()
        val lastPlayedAt = lastPlayedAtMillis[cue]
        if (lastPlayedAt != null && now - lastPlayedAt < mix.cooldownMillis) return

        val soundId = soundIds[cue] ?: return
        if (soundId !in loadedSoundIds) return

        val streamId = soundPool.play(
            soundId,
            mix.volume,
            mix.volume,
            1,
            0,
            mix.playbackRate,
        )
        if (streamId != 0) {
            lastPlayedAtMillis[cue] = now
            mix.ambientDucking?.let(ambientPlayback::duck)
        }
    }

    override fun setAmbient(track: AmbientTrack?) {
        if (released || currentAmbientTrack == track) return
        currentAmbientTrack = track
        applyAmbientState()
    }

    override suspend fun setEnabled(enabled: Boolean) {
        settingsRepository.setEnabled(enabled)
        if (!enabled) {
            soundPool.autoPause()
        }
        applyAmbientState(enabled = enabled)
    }

    override fun onAppForegrounded() {
        appForegrounded = true
        applyAmbientState()
    }

    override fun onAppBackgrounded() {
        appForegrounded = false
        applyAmbientState()
        soundPool.autoPause()
    }

    override fun release() {
        if (released) return
        released = true
        ambientPlayback.release()
        soundPool.release()
        scope.cancel()
    }

    private fun applyAmbientState(
        enabled: Boolean = settings.value.enabled,
    ) {
        if (released) return
        ambientPlayback.update(
            track = currentAmbientTrack,
            enabled = enabled,
            foregrounded = appForegrounded,
        )
    }
}
