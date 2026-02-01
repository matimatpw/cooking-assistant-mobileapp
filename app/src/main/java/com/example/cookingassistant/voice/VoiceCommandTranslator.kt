package com.example.cookingassistant.voice

import android.content.Context
import android.util.Log
import com.example.cookingassistant.R
import java.util.Locale

class VoiceCommandTranslator(private val context: Context) {

    private val commandPatterns: Map<VoiceCommand, List<String>> by lazy {
        loadCommandPatterns()
    }

    private fun loadCommandPatterns(): Map<VoiceCommand, List<String>> {
        return mapOf(
            VoiceCommand.NEXT to parsePatterns(R.string.voice_command_next),
            VoiceCommand.PREVIOUS to parsePatterns(R.string.voice_command_previous),
            VoiceCommand.REPEAT to parsePatterns(R.string.voice_command_repeat),
            VoiceCommand.START to parsePatterns(R.string.voice_command_start),
            VoiceCommand.INGREDIENTS to parsePatterns(R.string.voice_command_ingredients),
            VoiceCommand.DESCRIPTION to parsePatterns(R.string.voice_command_description),
            VoiceCommand.TIME to parsePatterns(R.string.voice_command_time),
            VoiceCommand.TIPS to parsePatterns(R.string.voice_command_tips),
            VoiceCommand.STEP_NUMBER to parsePatterns(R.string.voice_command_step),
            VoiceCommand.START_TIMER to parsePatterns(R.string.voice_command_start_timer),
            VoiceCommand.PAUSE_TIMER to parsePatterns(R.string.voice_command_pause_timer),
            VoiceCommand.RESUME_TIMER to parsePatterns(R.string.voice_command_resume_timer),
            VoiceCommand.STOP_TIMER to parsePatterns(R.string.voice_command_stop_timer),
            VoiceCommand.CHECK_TIMER to parsePatterns(R.string.voice_command_check_timer)
        )
    }

    private fun parsePatterns(resourceId: Int): List<String> {
        return context.getString(resourceId)
            .split("|")
            .map { it.trim().lowercase(Locale.getDefault()) }
    }

    fun translate(recognizedText: String): VoiceCommand? {
        val normalizedText = recognizedText.lowercase(Locale.getDefault()).trim()
        Log.d("VoiceCommandTranslator", "Translating: '$recognizedText' (normalized: '$normalizedText')")

        val result = commandPatterns.entries.firstOrNull { (command, patterns) ->
            val matched = patterns.any { pattern ->
                normalizedText.contains(pattern)
            }
            if (matched) {
                Log.d("VoiceCommandTranslator", "Matched command: $command")
            }
            matched
        }?.key

        if (result == null) {
            Log.d("VoiceCommandTranslator", "No command matched for: '$recognizedText'")
        }

        return result
    }

    fun getCommandHints(): Map<VoiceCommand, String> {
        return mapOf(
            VoiceCommand.NEXT to commandPatterns[VoiceCommand.NEXT]?.first().orEmpty(),
            VoiceCommand.PREVIOUS to commandPatterns[VoiceCommand.PREVIOUS]?.first().orEmpty(),
            VoiceCommand.REPEAT to commandPatterns[VoiceCommand.REPEAT]?.first().orEmpty(),
            VoiceCommand.START to commandPatterns[VoiceCommand.START]?.first().orEmpty(),
            VoiceCommand.INGREDIENTS to commandPatterns[VoiceCommand.INGREDIENTS]?.first().orEmpty(),
            VoiceCommand.DESCRIPTION to commandPatterns[VoiceCommand.DESCRIPTION]?.first().orEmpty(),
            VoiceCommand.TIME to commandPatterns[VoiceCommand.TIME]?.first().orEmpty(),
            VoiceCommand.TIPS to commandPatterns[VoiceCommand.TIPS]?.first().orEmpty(),
            VoiceCommand.STEP_NUMBER to commandPatterns[VoiceCommand.STEP_NUMBER]?.first().orEmpty()
        )
    }

    fun getPatternsForCommand(command: VoiceCommand): List<String> {
        return commandPatterns[command].orEmpty()
    }
}
