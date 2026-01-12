package com.example.cookingassistant.voice

import android.content.Context
import com.example.cookingassistant.R
import java.util.Locale

/**
 * Translates recognized speech to VoiceCommand based on device locale
 * Supports multiple languages by loading command patterns from string resources
 */
class VoiceCommandTranslator(private val context: Context) {

    private val commandPatterns: Map<VoiceCommand, List<String>> by lazy {
        loadCommandPatterns()
    }

    /**
     * Load command patterns from string resources
     * Patterns are pipe-separated in strings.xml (e.g., "next|forward|continue")
     */
    private fun loadCommandPatterns(): Map<VoiceCommand, List<String>> {
        return mapOf(
            VoiceCommand.NEXT to parsePatterns(R.string.voice_command_next),
            VoiceCommand.PREVIOUS to parsePatterns(R.string.voice_command_previous),
            VoiceCommand.REPEAT to parsePatterns(R.string.voice_command_repeat),
            VoiceCommand.START to parsePatterns(R.string.voice_command_start)
        )
    }

    /**
     * Parse pipe-separated patterns from string resource
     * Example: "next|forward|continue" → ["next", "forward", "continue"]
     */
    private fun parsePatterns(resourceId: Int): List<String> {
        return context.getString(resourceId)
            .split("|")
            .map { it.trim().lowercase(Locale.getDefault()) }
    }

    /**
     * Translate recognized text to VoiceCommand
     * @param recognizedText The text recognized by speech recognizer
     * @return VoiceCommand if match found, null otherwise
     */
    fun translate(recognizedText: String): VoiceCommand? {
        val normalizedText = recognizedText.lowercase(Locale.getDefault()).trim()

        // Find first command whose patterns match the recognized text
        return commandPatterns.entries.firstOrNull { (_, patterns) ->
            patterns.any { pattern ->
                normalizedText.contains(pattern)
            }
        }?.key
    }

    /**
     * Get localized command hints for display
     * @return Map of VoiceCommand to localized pattern examples
     */
    fun getCommandHints(): Map<VoiceCommand, String> {
        return mapOf(
            VoiceCommand.NEXT to commandPatterns[VoiceCommand.NEXT]?.first().orEmpty(),
            VoiceCommand.PREVIOUS to commandPatterns[VoiceCommand.PREVIOUS]?.first().orEmpty(),
            VoiceCommand.REPEAT to commandPatterns[VoiceCommand.REPEAT]?.first().orEmpty(),
            VoiceCommand.START to commandPatterns[VoiceCommand.START]?.first().orEmpty()
        )
    }

    /**
     * Get all patterns for a specific command (useful for testing)
     */
    fun getPatternsForCommand(command: VoiceCommand): List<String> {
        return commandPatterns[command].orEmpty()
    }
}
