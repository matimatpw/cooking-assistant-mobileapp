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

/**
 * Manages text-to-speech functionality for cooking mode
 * Handles Android TextToSpeech lifecycle and provides methods for speaking different types of recipe content
 */
class TextToSpeechManager(private val context: Context) {

    private val tag = "TextToSpeechManager"

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _status = MutableStateFlow<TtsStatus>(TtsStatus.IDLE)
    val status: StateFlow<TtsStatus> = _status.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    /**
     * Initialize TextToSpeech engine
     * @param onInitialized Callback invoked when TTS is ready to use
     */
    fun initialize(onInitialized: () -> Unit = {}) {
        if (tts == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    // Use app's selected language instead of system locale
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

    /**
     * Speak a step instruction
     * @param instruction The instruction text to speak
     */
    fun speakInstruction(instruction: String) {
        speak(instruction)
    }

    /**
     * Speak a list of ingredients
     * @param ingredients The ingredients to speak
     */
    fun speakIngredients(ingredients: List<Ingredient>) {
        Log.d(tag, "speakIngredients called with ${ingredients.size} ingredients")
        val text = formatIngredientsList(ingredients)
        Log.d(tag, "Formatted text: $text")
        speak(text)
    }

    /**
     * Speak a duration in minutes
     * @param minutes The duration in minutes
     */
    fun speakDuration(minutes: Int) {
        Log.d(tag, "speakDuration called with $minutes minutes")
        val text = formatDuration(minutes)
        Log.d(tag, "Formatted text: $text")
        speak(text)
    }

    /**
     * Speak cooking tips
     * @param tips The tips text to speak
     */
    fun speakTips(tips: String) {
        val prefix = context.getString(R.string.tts_tips_prefix)
        speak("$prefix $tips")
    }

    /**
     * Speak the current step number
     * @param stepNumber Current step number (1-indexed)
     * @param totalSteps Total number of steps
     */
    fun speakStepNumber(stepNumber: Int, totalSteps: Int) {
        val text = context.getString(R.string.tts_step_number, stepNumber, totalSteps)
        speak(text)
    }

    // ========== Timer TTS Methods ==========

    /**
     * Announce that a timer has been started
     * @param minutes Timer duration in minutes
     */
    fun speakTimerStarted(minutes: Int) {
        val text = context.getString(R.string.tts_timer_started, minutes)
        Log.d(tag, "Speaking timer started: $text")
        speak(text)
    }

    /**
     * Announce that a timer has been paused
     * @param minutes Remaining minutes
     * @param seconds Remaining seconds
     */
    fun speakTimerPaused(minutes: Int, seconds: Int) {
        val text = context.getString(R.string.tts_timer_paused, minutes, seconds)
        Log.d(tag, "Speaking timer paused: $text")
        speak(text)
    }

    /**
     * Announce that a timer has been resumed
     */
    fun speakTimerResumed() {
        val text = context.getString(R.string.tts_timer_resumed)
        Log.d(tag, "Speaking timer resumed: $text")
        speak(text)
    }

    /**
     * Announce that a timer has been cancelled
     */
    fun speakTimerCancelled() {
        val text = context.getString(R.string.tts_timer_cancelled)
        Log.d(tag, "Speaking timer cancelled: $text")
        speak(text)
    }

    /**
     * Announce that a timer has finished
     * @param stepNumber The step number (1-indexed) that finished
     */
    fun speakTimerFinished(stepNumber: Int) {
        val text = context.getString(R.string.tts_timer_finished, stepNumber)
        Log.d(tag, "Speaking timer finished: $text")
        speak(text)
    }

    /**
     * Announce the current timer status (remaining time)
     * @param minutes Remaining minutes
     * @param seconds Remaining seconds
     */
    fun speakTimerStatus(minutes: Int, seconds: Int) {
        val text = context.getString(R.string.tts_timer_status, minutes, seconds)
        Log.d(tag, "Speaking timer status: $text")
        speak(text)
    }

    /**
     * Speak a generic message (for timer error messages, etc.)
     * @param message The message to speak
     */
    fun speakMessage(message: String) {
        Log.d(tag, "Speaking message: $message")
        speak(message)
    }

    // ========== End Timer TTS Methods ==========

    /**
     * Stop current speech
     */
    fun stop() {
        if (isInitialized) {
            tts?.stop()
            _isSpeaking.value = false
            _status.value = TtsStatus.IDLE
            Log.d(tag, "Speech stopped")
        }
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        _isSpeaking.value = false
        _status.value = TtsStatus.IDLE
        Log.d(tag, "TextToSpeech destroyed")
    }

    /**
     * Speak text using TTS engine
     * @param text The text to speak
     * @param queueMode Queue mode (QUEUE_FLUSH interrupts current speech, QUEUE_ADD queues it)
     */
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

    /**
     * Format ingredients list for TTS
     * Example: "Spaghetti 400 grams. Pancetta 200 grams, cut into small cubes."
     */
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

    /**
     * Format duration for TTS
     * Example: "This step takes 10 minutes"
     */
    private fun formatDuration(minutes: Int): String {
        return if (minutes == 1) {
            context.getString(R.string.tts_duration_singular, minutes)
        } else {
            context.getString(R.string.tts_duration_plural, minutes)
        }
    }

    /**
     * Create utterance progress listener to track speaking status
     */
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

/**
 * TTS status enumeration
 */
enum class TtsStatus {
    /** TTS is idle and ready to speak */
    IDLE,
    /** TTS is currently speaking */
    SPEAKING,
    /** TTS encountered an error */
    ERROR
}
