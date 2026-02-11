package com.lux.field.domain.voice

import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Playing(val messageId: String) : PlaybackState
    data class Error(val message: String) : PlaybackState
}

class AudioPlayerManager(
    private val context: Context,
) {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var tempFile: File? = null

    fun play(messageId: String, audioBytes: ByteArray) {
        stop()

        try {
            val file = File(context.cacheDir, "tts_$messageId.mp3").also {
                it.writeBytes(audioBytes)
            }
            tempFile = file

            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    _state.value = PlaybackState.Playing(messageId)
                }
                setOnCompletionListener {
                    cleanup()
                    _state.value = PlaybackState.Idle
                }
                setOnErrorListener { _, what, extra ->
                    cleanup()
                    _state.value = PlaybackState.Error("Playback error ($what/$extra)")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            cleanup()
            _state.value = PlaybackState.Error(e.message ?: "Playback failed")
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
        }
        cleanup()
        _state.value = PlaybackState.Idle
    }

    fun destroy() {
        stop()
    }

    private fun cleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
        tempFile?.delete()
        tempFile = null
    }
}
