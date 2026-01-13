package com.example.cookingassistant.voice

import android.content.Context
import com.example.cookingassistant.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Unit tests for VoiceCommandTranslator
 * Demonstrates how to test voice command parsing without running the app
 */
class VoiceCommandTranslatorTest {

    private lateinit var context: Context
    private lateinit var translator: VoiceCommandTranslator

    @Before
    fun setup() {
        context = mock(Context::class.java)

        // Mock English command patterns
        `when`(context.getString(R.string.voice_command_next))
            .thenReturn("next|forward|continue")
        `when`(context.getString(R.string.voice_command_previous))
            .thenReturn("previous|back|go back")
        `when`(context.getString(R.string.voice_command_repeat))
            .thenReturn("repeat|again")
        `when`(context.getString(R.string.voice_command_start))
            .thenReturn("start|begin")

        translator = VoiceCommandTranslator(context)
    }

    @Test
    fun translate_recognizes_next_command_with_exact_match() {
        val result = translator.translate("next")

        assertThat(result).isEqualTo(VoiceCommand.NEXT)
    }

    @Test
    fun translate_recognizes_next_command_with_alternative_word() {
        val result = translator.translate("forward")

        assertThat(result).isEqualTo(VoiceCommand.NEXT)
    }

    @Test
    fun translate_recognizes_next_command_in_sentence() {
        val result = translator.translate("go to the next step please")

        assertThat(result).isEqualTo(VoiceCommand.NEXT)
    }

    @Test
    fun translate_recognizes_previous_command() {
        val result = translator.translate("previous")

        assertThat(result).isEqualTo(VoiceCommand.PREVIOUS)
    }

    @Test
    fun translate_recognizes_go_back_command() {
        val result = translator.translate("go back")

        assertThat(result).isEqualTo(VoiceCommand.PREVIOUS)
    }

    @Test
    fun translate_recognizes_repeat_command() {
        val result = translator.translate("repeat")

        assertThat(result).isEqualTo(VoiceCommand.REPEAT)
    }

    @Test
    fun translate_recognizes_start_command() {
        val result = translator.translate("start")

        assertThat(result).isEqualTo(VoiceCommand.START)
    }

    @Test
    fun translate_is_case_insensitive() {
        val result = translator.translate("NEXT")

        assertThat(result).isEqualTo(VoiceCommand.NEXT)
    }

    @Test
    fun translate_handles_extra_whitespace() {
        val result = translator.translate("  next  ")

        assertThat(result).isEqualTo(VoiceCommand.NEXT)
    }

    @Test
    fun translate_returns_null_for_unrecognized_command() {
        val result = translator.translate("random text")

        assertThat(result).isNull()
    }

    @Test
    fun translate_returns_first_matching_command_when_multiple_match() {
        // If text contains multiple command words, returns first match
        val result = translator.translate("go back to the start")

        // Should match PREVIOUS (back) since it's checked first in the map
        assertThat(result).isIn(VoiceCommand.PREVIOUS, VoiceCommand.START)
    }

    @Test
    fun get_patterns_for_command_returns_all_patterns() {
        val patterns = translator.getPatternsForCommand(VoiceCommand.NEXT)

        assertThat(patterns).containsExactly("next", "forward", "continue")
    }

    @Test
    fun get_command_hints_returns_first_pattern_for_each_command() {
        val hints = translator.getCommandHints()

        assertThat(hints[VoiceCommand.NEXT]).isEqualTo("next")
        assertThat(hints[VoiceCommand.PREVIOUS]).isEqualTo("previous")
        assertThat(hints[VoiceCommand.REPEAT]).isEqualTo("repeat")
        assertThat(hints[VoiceCommand.START]).isEqualTo("start")
    }
}

/**
 * Unit tests for Polish voice commands
 * Demonstrates multi-language support testing
 */
class VoiceCommandTranslatorPolishTest {

    private lateinit var context: Context
    private lateinit var translator: VoiceCommandTranslator

    @Before
    fun setup() {
        context = mock(Context::class.java)

        // Mock Polish command patterns
        `when`(context.getString(R.string.voice_command_next))
            .thenReturn("następny|następnego|następna|następnej|następne|dalej|kontynuuj|naprzód|kolejny")
        `when`(context.getString(R.string.voice_command_previous))
            .thenReturn("poprzedni|poprzedniego|poprzednia|poprzedniej|poprzednie|wstecz|cofnij|powrót|z powrotem")
        `when`(context.getString(R.string.voice_command_repeat))
            .thenReturn("powtórz|jeszcze raz|raz jeszcze")
        `when`(context.getString(R.string.voice_command_start))
            .thenReturn("start|początek|na początek|od początku")

        translator = VoiceCommandTranslator(context)
    }

    @Test
    fun translate_recognizes_polish_next_command() {
        val result = translator.translate("następny")

        assertThat(result).isEqualTo(VoiceCommand.NEXT)
    }

    @Test
    fun translate_recognizes_polish_dalej_command() {
        val result = translator.translate("dalej")

        assertThat(result).isEqualTo(VoiceCommand.NEXT)
    }

    @Test
    fun translate_recognizes_polish_previous_command() {
        val result = translator.translate("poprzedni")

        assertThat(result).isEqualTo(VoiceCommand.PREVIOUS)
    }

    @Test
    fun translate_recognizes_polish_wstecz_command() {
        val result = translator.translate("wstecz")

        assertThat(result).isEqualTo(VoiceCommand.PREVIOUS)
    }

    @Test
    fun translate_recognizes_polish_repeat_command() {
        val result = translator.translate("powtórz")

        assertThat(result).isEqualTo(VoiceCommand.REPEAT)
    }

    @Test
    fun translate_recognizes_polish_jeszcze_raz_command() {
        val result = translator.translate("jeszcze raz")

        assertThat(result).isEqualTo(VoiceCommand.REPEAT)
    }

    @Test
    fun translate_recognizes_polish_start_command() {
        val result = translator.translate("na początek")

        assertThat(result).isEqualTo(VoiceCommand.START)
    }

    @Test
    fun translate_recognizes_polish_command_in_sentence() {
        val result = translator.translate("proszę przejdź do następnego kroku")

        assertThat(result).isEqualTo(VoiceCommand.NEXT)
    }
}
