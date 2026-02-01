package com.example.cookingassistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.cookingassistant.util.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceCommandManager(private val context: Context) {

    private val tag = "VoiceCommandManager"

    private var speechRecognizer: SpeechRecognizer? = null

    private fun createRecognizerIntent(): Intent {
        val appLanguage = LocaleManager.getCurrentLanguage(context)
        val locale = Locale.forLanguageTag(appLanguage)

        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    private val commandTranslator = VoiceCommandTranslator(context)

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow<String?>(null)
    val recognizedText: StateFlow<String?> = _recognizedText.asStateFlow()

    private val _allRecognizedText = MutableStateFlow<List<String>>(emptyList())
    val allRecognizedText: StateFlow<List<String>> = _allRecognizedText.asStateFlow()

    private var onCommandCallback: ((VoiceCommand) -> Unit)? = null
    private var onAutoRestartCallback: (() -> Unit)? = null
    private var autoRestartEnabled = false
    private var manuallyPaused = false

    fun initialize() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
            Log.d(tag, "SpeechRecognizer initialized")
        }
    }

    fun clearRecognizedTextHistory() {
        _allRecognizedText.value = emptyList()
        _recognizedText.value = null
    }

    fun startListening(
        onCommand: (VoiceCommand) -> Unit,
        enableAutoRestart: Boolean = false,
        onAutoRestart: (() -> Unit)? = null
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(tag, "Speech recognition not available on this device")
            return
        }

        onCommandCallback = onCommand
        onAutoRestartCallback = onAutoRestart
        autoRestartEnabled = enableAutoRestart
        manuallyPaused = false
        _isListening.value = true
        _recognizedText.value = null

        val intent = createRecognizerIntent()
        speechRecognizer?.startListening(intent)
        Log.d(tag, "Started listening for voice commands (auto-restart: $enableAutoRestart, language: ${LocaleManager.getCurrentLanguage(context)})")
    }

    fun stopListening(isPause: Boolean = false) {
        speechRecognizer?.stopListening()
        _isListening.value = false
        if (isPause) {
            manuallyPaused = true
            Log.d(tag, "Manually paused voice commands")
        } else {
            Log.d(tag, "Stopped listening for voice commands")
        }
    }

    fun cancel() {
        speechRecognizer?.cancel()
        _isListening.value = false
        manuallyPaused = true
        Log.d(tag, "Voice recognition cancelled")
    }

    fun resume() {
        if (manuallyPaused) {
            manuallyPaused = false
            onCommandCallback?.let { callback ->
                startListening(
                    onCommand = callback,
                    enableAutoRestart = autoRestartEnabled,
                    onAutoRestart = onAutoRestartCallback
                )
            }
        }
    }

    fun isPaused(): Boolean = manuallyPaused

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        onCommandCallback = null
        Log.d(tag, "VoiceCommandManager destroyed")
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(tag, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(tag, "Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {
        }

        override fun onBufferReceived(buffer: ByteArray?) {
        }

        override fun onEndOfSpeech() {
            Log.d(tag, "Speech ended")
            _isListening.value = false
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                SpeechRecognizer.ERROR_SERVER -> "Server sends error status"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error: $error"
            }
            Log.e(tag, "Recognition error: $errorMessage")
            _isListening.value = false

            if (autoRestartEnabled && !manuallyPaused) {
                when (error) {
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        Log.d(tag, "Auto-restarting after recoverable error")
                        onAutoRestartCallback?.invoke()
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0].lowercase(Locale.getDefault())
                Log.d(tag, "Recognized: $recognizedText")
                _recognizedText.value = recognizedText

                _allRecognizedText.value = _allRecognizedText.value + recognizedText

                val command = parseCommand(recognizedText)
                command?.let { onCommandCallback?.invoke(it) }

                if (autoRestartEnabled && !manuallyPaused) {
                    Log.d(tag, "Auto-restarting listening after command")
                    onAutoRestartCallback?.invoke()
                }
            }
            _isListening.value = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val partialText = matches[0].lowercase(Locale.getDefault())
                Log.d(tag, "Partial result: $partialText")
                _recognizedText.value = partialText
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
        }
    }

    private fun parseCommand(text: String): VoiceCommand? {
        val command = commandTranslator.translate(text)
        if (command == null) {
            Log.d(tag, "No matching command for: $text")
        }
        return command
    }

    fun getCommandHints(): Map<VoiceCommand, String> {
        return commandTranslator.getCommandHints()
    }
}

enum class VoiceCommand {
    NEXT,           // Go to next step
    PREVIOUS,       // Go to previous step
    REPEAT,         // Repeat current step
    START,          // Go to first step

    INGREDIENTS,    // Read step ingredients
    DESCRIPTION,    // Read step instruction
    TIME,           // Read step duration
    TIPS,           // Read step tips
    STEP_NUMBER,    // Read current step number

    START_TIMER,    // Start timer for current step
    PAUSE_TIMER,    // Pause active timer
    RESUME_TIMER,   // Resume paused timer
    STOP_TIMER,     // Stop/cancel timer
    CHECK_TIMER     // Check timer status (read remaining time)
}
