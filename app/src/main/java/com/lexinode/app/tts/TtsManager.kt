package com.lexinode.app.tts

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import com.lexinode.app.ui.Bidi
import java.util.Locale

enum class SpeakResult {
    OK,
    HEBREW_VOICE_MISSING,
    LANGUAGE_MISSING,
    ENGINE_UNAVAILABLE,
}

/** Wrapper around Android TextToSpeech that picks Hebrew or the default locale by text. */
class TtsManager(context: Context) {
    private var ready = false
    private var failed = false
    private var pending: Pair<String, (SpeakResult) -> Unit>? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        onInit(status)
    }

    private fun onInit(status: Int) {
        val request = pending
        pending = null
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            request?.let { (text, callback) -> callback(speakNow(text)) }
        } else {
            failed = true
            request?.second?.invoke(SpeakResult.ENGINE_UNAVAILABLE)
        }
    }

    fun speak(text: String, onResult: (SpeakResult) -> Unit) {
        when {
            failed -> onResult(SpeakResult.ENGINE_UNAVAILABLE)
            ready -> onResult(speakNow(text))
            else -> pending = text to onResult
        }
    }

    private fun speakNow(text: String): SpeakResult {
        try {
            if (Bidi.containsHebrew(text)) {
                val hebrew = Locale.forLanguageTag("he-IL")
                if (tts.isLanguageAvailable(hebrew) < TextToSpeech.LANG_AVAILABLE) {
                    return SpeakResult.HEBREW_VOICE_MISSING
                }
                if (tts.setLanguage(hebrew) < TextToSpeech.LANG_AVAILABLE) {
                    return SpeakResult.HEBREW_VOICE_MISSING
                }
                val voice = try {
                    tts.voice
                } catch (e: Exception) {
                    null
                }
                if (voice != null) {
                    val notInstalled = voice.features?.contains(
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED,
                    ) == true
                    val lang = voice.locale?.language
                    if (notInstalled || (lang != null && lang != "he" && lang != "iw")) {
                        return SpeakResult.HEBREW_VOICE_MISSING
                    }
                }
            } else {
                if (tts.setLanguage(Locale.getDefault()) < TextToSpeech.LANG_AVAILABLE) {
                    val fallback = try {
                        tts.defaultVoice
                    } catch (e: Exception) {
                        null
                    }
                    if (fallback == null) return SpeakResult.LANGUAGE_MISSING
                    tts.voice = fallback
                }
            }
            val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lexinode-${System.nanoTime()}")
            return if (result == TextToSpeech.SUCCESS) SpeakResult.OK else SpeakResult.ENGINE_UNAVAILABLE
        } catch (e: Exception) {
            return SpeakResult.ENGINE_UNAVAILABLE
        }
    }

    fun shutdown() {
        pending = null
        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {
        }
    }

    companion object {
        fun openInstallVoiceData(context: Context) {
            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                openTtsSettings(context)
            }
        }

        fun openTtsSettings(context: Context) {
            val intent = Intent("com.android.settings.TTS_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } catch (_: ActivityNotFoundException) {
                }
            }
        }
    }
}
