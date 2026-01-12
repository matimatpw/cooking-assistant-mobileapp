package com.example.cookingassistant.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Manages app language/locale preferences
 * Handles storing and applying language settings
 */
object LocaleManager {
    private const val PREFS_NAME = "app_preferences"
    private const val KEY_LANGUAGE = "selected_language"
    private const val DEFAULT_LANGUAGE = "en"

    /**
     * Supported languages
     */
    enum class Language(val code: String, val displayName: String) {
        ENGLISH("en", "English"),
        POLISH("pl", "Polski")
    }

    /**
     * Get current language setting
     * @param context Application context
     * @return Current language code
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * Set and apply new language
     * @param context Application context
     * @param languageCode Language code (e.g., "en", "pl")
     */
    fun setLanguage(context: Context, languageCode: String) {
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    /**
     * Apply the saved language to the context
     * @param context Application context
     * @return Updated context with applied locale
     */
    fun applyLanguage(context: Context): Context {
        val language = getCurrentLanguage(context)
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }

    /**
     * Get all supported languages
     * @return List of supported languages
     */
    fun getSupportedLanguages(): List<Language> {
        return Language.entries
    }

    /**
     * Get SharedPreferences instance
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
