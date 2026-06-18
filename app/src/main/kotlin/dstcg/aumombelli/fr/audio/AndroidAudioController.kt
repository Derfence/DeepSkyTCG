package fr.aumombelli.dstcg.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
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
        .setMaxStreams(4)
        .setAudioAttributes(audioAttributes)
        .build()
    private val loadedSoundIds = mutableSetOf<Int>()
    private val soundIds = mutableMapOf<SoundCue, Int>()
    private var ambientPlayer: MediaPlayer? = null
    private var preparedAmbientTrack: AmbientTrack? = null
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
        SoundCue.entries.associateWithTo(soundIds) { cue ->
            appContext.assets.openFd(cue.assetPath).use { asset ->
                soundPool.load(asset, 1)
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
        val soundId = soundIds[cue] ?: return
        if (soundId !in loadedSoundIds) return

        soundPool.play(
            soundId,
            cue.volume,
            cue.volume,
            1,
            0,
            cue.playbackRate,
        )
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
    }

    override fun onAppForegrounded() {
        appForegrounded = true
        applyAmbientState()
    }

    override fun onAppBackgrounded() {
        appForegrounded = false
        ambientPlayer?.pause()
        soundPool.autoPause()
    }

    override fun release() {
        if (released) return
        released = true
        ambientPlayer?.release()
        ambientPlayer = null
        soundPool.release()
        scope.cancel()
    }

    private fun applyAmbientState() {
        if (released) return
        val track = currentAmbientTrack
        if (!settings.value.enabled || !appForegrounded || track == null) {
            ambientPlayer?.pause()
            return
        }

        val player = ambientPlayer?.takeIf { preparedAmbientTrack == track } ?: createAmbientPlayer(track)
        ambientPlayer = player
        if (!player.isPlaying) {
            runCatching { player.start() }
        }
    }

    private fun createAmbientPlayer(track: AmbientTrack): MediaPlayer {
        ambientPlayer?.release()
        preparedAmbientTrack = track
        return MediaPlayer().apply {
            setAudioAttributes(ambientAudioAttributes)
            appContext.assets.openFd(track.assetPath).use { asset ->
                setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
            }
            isLooping = true
            setVolume(track.volume, track.volume)
            prepare()
        }
    }
}

private val SoundCue.volume: Float
    get() = when (this) {
        SoundCue.UiNavigate -> 0.34f
        SoundCue.LibraryOpen -> 0.38f
        SoundCue.LibraryClose -> 0.34f
        SoundCue.EquipmentOpen -> 0.38f
        SoundCue.EquipmentClose -> 0.34f
        SoundCue.BadgeBookOpen -> 0.38f
        SoundCue.BadgeBookClose -> 0.34f
        SoundCue.PackBurst -> 0.48f
        SoundCue.PackReveal -> 0.42f
        SoundCue.HolographicReveal -> 0.48f
        SoundCue.MiniGameSuccess -> 0.38f
        SoundCue.MiniGameError -> 0.34f
        SoundCue.MiniGameCompletion -> 0.52f
        SoundCue.BadgeUnlock -> 0.48f
    }

private val SoundCue.playbackRate: Float
    get() = when (this) {
        SoundCue.MiniGameError -> 0.92f
        SoundCue.MiniGameCompletion -> 1.04f
        else -> 1f
    }

private val AmbientTrack.volume: Float
    get() = when (this) {
        AmbientTrack.Starfield -> 0.20f
        AmbientTrack.MiniGames -> 0.22f
    }
