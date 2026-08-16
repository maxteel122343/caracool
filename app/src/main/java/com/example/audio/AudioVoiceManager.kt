package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

data class SpeakRequest(
    val text: String,
    val pitch: Float = 1.05f,
    val speechRate: Float = 1.0f,
    val onDone: (() -> Unit)? = null
)

class AudioVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isTtsReady = false
    private val pendingRequests = mutableListOf<SpeakRequest>()

    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecording = false

    companion object {
        @Volatile
        private var INSTANCE: AudioVoiceManager? = null

        fun getInstance(context: Context): AudioVoiceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioVoiceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("AudioVoiceManager", "Failed to construct TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val ptLocale = Locale("pt", "BR")
            val result = tts?.setLanguage(ptLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setPitch(1.08f) // Friendly bright pitch
            tts?.setSpeechRate(1.02f)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts?.setAudioAttributes(audioAttributes)
                } catch (e: Exception) {
                    Log.e("AudioVoiceManager", "Error setting audio attributes", e)
                }
            }

            isTtsReady = true
            Log.d("AudioVoiceManager", "TTS successfully initialized with pt-BR locale.")

            // Drain queued speech requests
            synchronized(pendingRequests) {
                for (req in pendingRequests) {
                    speakInternal(req.text, req.pitch, req.speechRate, req.onDone)
                }
                pendingRequests.clear()
            }
        } else {
            Log.e("AudioVoiceManager", "TTS initialization failed with code: $status")
        }
    }

    /**
     * Converts a number into Brazilian Portuguese ordinal words (e.g. 15 -> "décima quinta")
     */
    fun toPortugueseOrdinal(number: Int): String {
        if (number <= 0) return "primeira"

        val units = arrayOf(
            "", "primeira", "segunda", "terceira", "quarta", "quinta",
            "sexta", "sétima", "oitava", "nona"
        )
        val tens = arrayOf(
            "", "décima", "vigésima", "trigésima", "quadragésima", "quinquagésima",
            "sexagésima", "septuagésima", "octogésima", "nonagésima"
        )
        val hundreds = arrayOf(
            "", "centésima", "ducentésima", "trecentésima", "quadringentésima",
            "quingentésima", "seiscentésima", "septingentésima", "octingentésima", "nongentésima"
        )

        return when {
            number in 1..9 -> units[number]
            number in 10..99 -> {
                val t = number / 10
                val u = number % 10
                if (u == 0) tens[t] else "${tens[t]} ${units[u]}"
            }
            number in 100..999 -> {
                val h = number / 100
                val rem = number % 100
                if (rem == 0) hundreds[h] else "${hundreds[h]} ${toPortugueseOrdinal(rem)}"
            }
            else -> "${number}ª"
        }
    }

    /**
     * Generates the unlock phrase to be spoken according to app configuration, incorporating user name
     */
    fun formatUnlockPhrase(
        userName: String = "",
        countToday: Int,
        audioType: String,
        customText: String,
        isAiNatural: Boolean,
        isKoolMode: Boolean = false,
        isSafeWordMode: Boolean = true
    ): String {
        val defaultName = if (isKoolMode) {
            if (isSafeWordMode) "Cara de Cool" else "Cara de Cu"
        } else "Cara de Paçoca"
        val mascotName = if (isKoolMode) {
            if (isSafeWordMode) "Cara de cool" else "Cara de cu"
        } else "Cara de paçoca"
        val cleanName = if (userName.isNotBlank() && userName != "Você (Cara de Paçoca)" && userName != "Você (Cara de Cu)" && userName != "Você (Cara de Kool)" && userName != "Você (Cara de Cool)" && userName != defaultName) userName.trim() else ""
        val namePrefix = if (cleanName.isNotBlank()) "$cleanName, " else ""

        val rawPhrase = when (audioType) {
            "tts_standard" -> "${namePrefix}$mascotName desbloqueado!"
            "tts_counter" -> {
                val ordinal = toPortugueseOrdinal(countToday)
                "${namePrefix}$mascotName desbloqueado pela $ordinal vez e contando!"
            }
            "tts_custom" -> {
                val ordinal = toPortugueseOrdinal(countToday)
                var resolvedCustom = customText
                    .replace("{nome}", cleanName.ifBlank { defaultName })
                    .replace("{name}", cleanName.ifBlank { defaultName })
                    .replace("{usuario}", cleanName.ifBlank { defaultName })
                    .replace("{vez}", ordinal)
                    .replace("{count}", countToday.toString())
                    .replace("{numero}", countToday.toString())

                if (isKoolMode) {
                    val cuReplacement = if (isSafeWordMode) "cara de cool" else "cara de cu"
                    resolvedCustom = resolvedCustom
                        .replace("cara de paçoca", cuReplacement, ignoreCase = true)
                        .replace("paçoca", if (isSafeWordMode) "cool" else "cu", ignoreCase = true)
                        .replace("cara de kool", cuReplacement, ignoreCase = true)
                        .replace("kool", if (isSafeWordMode) "cool" else "cu", ignoreCase = true)
                }

                if (resolvedCustom.isNotBlank()) {
                    if (cleanName.isNotBlank() && !resolvedCustom.contains(cleanName, ignoreCase = true)) {
                        "${cleanName}, $resolvedCustom"
                    } else {
                        resolvedCustom
                    }
                } else {
                    "${namePrefix}$mascotName desbloqueado pela $ordinal vez e contando!"
                }
            }
            else -> {
                val ordinal = toPortugueseOrdinal(countToday)
                "${namePrefix}$mascotName desbloqueado pela $ordinal vez e contando!"
            }
        }

        return com.example.util.SafeWordHelper.formatSafeWord(rawPhrase, isSafeWordMode)
    }

    /**
     * Speaks the given phrase using TTS (queues if initializing)
     */
    fun speak(text: String, pitch: Float = 1.05f, speechRate: Float = 1.0f, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) return

        if (!isTtsReady || tts == null) {
            Log.d("AudioVoiceManager", "TTS not ready yet, queuing: $text")
            synchronized(pendingRequests) {
                pendingRequests.add(SpeakRequest(text, pitch, speechRate, onDone))
            }
            if (tts == null) {
                try {
                    tts = TextToSpeech(context.applicationContext, this)
                } catch (_: Exception) {}
            }
            return
        }

        speakInternal(text, pitch, speechRate, onDone)
    }

    private fun speakInternal(text: String, pitch: Float, speechRate: Float, onDone: (() -> Unit)?) {
        try {
            tts?.setPitch(pitch)
            tts?.setSpeechRate(speechRate)

            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            val utteranceId = "utterance_${System.currentTimeMillis()}"

            if (onDone != null) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        CoroutineScope(Dispatchers.Main).launch { onDone() }
                    }
                    override fun onError(utteranceId: String?) {
                        CoroutineScope(Dispatchers.Main).launch { onDone() }
                    }
                })
            }

            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            Log.d("AudioVoiceManager", "Speaking out loud: $text")
        } catch (e: Exception) {
            Log.e("AudioVoiceManager", "Error in speakInternal: ${e.message}", e)
        }
    }

    /**
     * Plays unlock sound / voice based on user preference
     */
    fun playUnlockAudio(
        userName: String = "",
        countToday: Int,
        audioType: String,
        customText: String,
        isAiNatural: Boolean,
        customAudioUri: String?,
        coroutineScope: CoroutineScope,
        isKoolMode: Boolean = false,
        isSafeWordMode: Boolean = true
    ) {
        when (audioType) {
            "sound_chime" -> {
                coroutineScope.launch { SoundSynth.playChime() }
            }
            "sound_cyber" -> {
                coroutineScope.launch { SoundSynth.playCyberTone() }
            }
            "sound_pop" -> {
                coroutineScope.launch { SoundSynth.playPopTone() }
            }
            "custom_recording" -> {
                if (!customAudioUri.isNullOrBlank()) {
                    playAudioFile(customAudioUri)
                } else {
                    val phrase = formatUnlockPhrase(userName, countToday, "tts_counter", customText, isAiNatural, isKoolMode, isSafeWordMode)
                    speak(phrase)
                }
            }
            else -> {
                val phrase = formatUnlockPhrase(userName, countToday, audioType, customText, isAiNatural, isKoolMode, isSafeWordMode)
                speak(phrase)
            }
        }
    }

    /**
     * Plays audio when locked
     */
    fun playLockAudio(customLockText: String?, coroutineScope: CoroutineScope) {
        coroutineScope.launch { SoundSynth.playLockTone() }
        if (!customLockText.isNullOrBlank() && customLockText != "Desativado") {
            speak(customLockText, pitch = 0.95f)
        }
    }

    fun playAudioFile(uriString: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                    setDataSource(context, Uri.parse(uriString))
                } else {
                    setDataSource(uriString)
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AudioVoiceManager", "Error playing audio file: ${e.message}")
        }
    }

    fun startRecording(outputPath: String): Boolean {
        return try {
            recordingFile = File(outputPath)
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            true
        } catch (e: Exception) {
            Log.e("AudioVoiceManager", "Error starting recording: ${e.message}")
            false
        }
    }

    fun stopRecording(): String? {
        return try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                recordingFile?.absolutePath
            } else null
        } catch (e: Exception) {
            Log.e("AudioVoiceManager", "Error stopping recording: ${e.message}")
            null
        }
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
        mediaPlayer?.release()
        mediaPlayer = null
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
