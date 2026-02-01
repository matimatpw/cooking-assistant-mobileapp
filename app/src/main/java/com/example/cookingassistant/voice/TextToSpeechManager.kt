package com.example.cookingassistant.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.cookingassistant.R
import com.example.cookingassistant.model.Ingredient
import com.example.cookingassistant.util.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) {

    private val tag = "TextToSpeechManager"

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _status = MutableStateFlow<TtsStatus>(TtsStatus.IDLE)
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun initialize(onInitialized: () -> Unit = {}) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val appLanguage = LocaleManager.getCurrentLanguage(context)
                    val locale = Locale.forLanguageTag(appLanguage)

                    val result = tts?.setLanguage(locale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(tag, "Language not supported: $locale (app language: $appLanguage)")
                        _status.value = TtsStatus.ERROR
                    } else {
                        isInitialized = true
                        _status.value = TtsStatus.IDLE
                        tts?.setOnUtteranceProgressListener(createUtteranceProgressListener())
                        Log.d(tag, "TextToSpeech initialized successfully with language: $locale (app language: $appLanguage)")
                        onInitialized()
                    }
                } else {
                    Log.e(tag, "TextToSpeech initialization failed")
                    _status.value = TtsStatus.ERROR
                }
            }
        }
    }

    fun speakInstruction(instruction: String) {
        speak(instruction)
    }

    fun speakIngredients(ingredients: List<Ingredient>) {
        Log.d(tag, "speakIngredients called with ${ingredients.size} ingredients")
        val text = formatIngredientsList(ingredients)
        Log.d(tag, "Formatted text: $text")
        speak(text)
    }

    fun speakDuration(minutes: Int) {
        Log.d(tag, "speakDuration called with $minutes minutes")
        val text = formatDuration(minutes)
        Log.d(tag, "Formatted text: $text")
        speak(text)
    }

    fun speakTips(tips: String) {
        val prefix = context.getString(R.string.tts_tips_prefix)
        speak("$prefix $tips")
    }

    fun speakStepNumber(stepNumber: Int, totalSteps: Int) {
        val text = context.getString(R.string.tts_step_number, stepNumber, totalSteps)
        speak(text)
    }

    fun speakTimerStarted(minutes: Int) {
        val text = context.getString(R.string.tts_timer_started, minutes)
        Log.d(tag, "Speaking timer started: $text")
        speak(text)
    }

    fun speakTimerPaused(minutes: Int, seconds: Int) {
        val text = context.getString(R.string.tts_timer_paused, minutes, seconds)
        Log.d(tag, "Speaking timer paused: $text")
        speak(text)
    }

    fun speakTimerResumed() {
        val text = context.getString(R.string.tts_timer_resumed)
        Log.d(tag, "Speaking timer resumed: $text")
        speak(text)
    }

    fun speakTimerCancelled() {
        val text = context.getString(R.string.tts_timer_cancelled)
        Log.d(tag, "Speaking timer cancelled: $text")
        speak(text)
    }

    fun speakTimerFinished(stepNumber: Int) {
        val text = context.getString(R.string.tts_timer_finished, stepNumber)
        Log.d(tag, "Speaking timer finished: $text")
        speak(text)
    }

    fun speakTimerStatus(minutes: Int, seconds: Int) {
        val text = context.getString(R.string.tts_timer_status, minutes, seconds)
        Log.d(tag, "Speaking timer status: $text")
        speak(text)
    }

    fun speakMessage(message: String) {
        Log.d(tag, "Speaking message: $message")
        speak(message)
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            _isSpeaking.value = false
            _status.value = TtsStatus.IDLE
            Log.d(tag, "Speech stopped")
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isSpeaking.value = false
        _status.value = TtsStatus.IDLE
        Log.d(tag, "TextToSpeech destroyed")
    }

    private fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized || tts == null) {
            Log.w(tag, "Cannot speak: TTS not initialized")
            return
        }

        if (text.isBlank()) {
            Log.w(tag, "Cannot speak: text is empty")
            return
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_${System.currentTimeMillis()}")
        }

        tts?.speak(text, queueMode, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
        Log.d(tag, "Speaking: $text")
    }

    private fun formatIngredientsList(ingredients: List<Ingredient>): String {
        if (ingredients.isEmpty()) {
            return context.getString(R.string.tts_no_ingredients)
        }

        val prefix = context.getString(R.string.tts_ingredients_prefix)
        val ingredientsText = ingredients.joinToString(separator = ". ") { ingredient ->
            buildString {
                append(ingredient.name)
                append(" ")
                append(ingredient.quantity)
                ingredient.notes?.let { notes ->
                    append(", ")
                    append(notes)
                }
            }
        }

        return "$prefix $ingredientsText."
    }

    private fun formatDuration(minutes: Int): String {
        return if (minutes == 1) {
            context.getString(R.string.tts_duration_singular, minutes)
        } else {
            context.getString(R.string.tts_duration_plural, minutes)
        }
    }

    private fun createUtteranceProgressListener() = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _isSpeaking.value = true
            _status.value = TtsStatus.SPEAKING
            Log.d(tag, "Speech started: $utteranceId")
        }

        override fun onDone(utteranceId: String?) {
            _isSpeaking.value = false
            _status.value = TtsStatus.IDLE
            Log.d(tag, "Speech completed: $utteranceId")
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            _isSpeaking.value = false
            _status.value = TtsStatus.ERROR
            Log.e(tag, "Speech error: $utteranceId")
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            _isSpeaking.value = false
            _status.value = TtsStatus.ERROR
            Log.e(tag, "Speech error: $utteranceId, code: $errorCode")
        }
    }
}

enum class TtsStatus {
    IDLE,
    SPEAKING,
    ERROR
}
