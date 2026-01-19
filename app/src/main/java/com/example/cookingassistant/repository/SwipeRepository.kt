package com.example.cookingassistant.repository

import android.content.Context
import com.example.cookingassistant.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Repository for managing swipe actions, history, and preferences
 */
class SwipeRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val swipeHistoryFile = File(context.filesDir, "swipe_history.json")
    private val swipePreferencesFile = File(context.filesDir, "swipe_preferences.json")

    private val _swipeHistory = MutableStateFlow(loadSwipeHistory())
    val swipeHistory: Flow<SwipeHistory> = _swipeHistory.asStateFlow()

    private val _swipePreferences = MutableStateFlow(loadSwipePreferences())
    val swipePreferences: Flow<SwipePreferences> = _swipePreferences.asStateFlow()

    /**
     * Load swipe history from file
     */
    private fun loadSwipeHistory(): SwipeHistory {
        return try {
            if (swipeHistoryFile.exists()) {
                val jsonString = swipeHistoryFile.readText()
                json.decodeFromString<SwipeHistory>(jsonString)
            } else {
                SwipeHistory()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SwipeHistory()
        }
    }

    /**
     * Save swipe history to file
     */
    private fun saveSwipeHistory(history: SwipeHistory) {
        try {
            val jsonString = json.encodeToString(history)
            swipeHistoryFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Load swipe preferences from file
     */
    private fun loadSwipePreferences(): SwipePreferences {
        return try {
            if (swipePreferencesFile.exists()) {
                val jsonString = swipePreferencesFile.readText()
                json.decodeFromString<SwipePreferences>(jsonString)
            } else {
                SwipePreferences()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SwipePreferences()
        }
    }

    /**
     * Save swipe preferences to file
     */
    private fun saveSwipePreferences(preferences: SwipePreferences) {
        try {
            val jsonString = json.encodeToString(preferences)
            swipePreferencesFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Record a swipe action
     */
    fun recordSwipe(action: SwipeAction) {
        _swipeHistory.update { currentHistory ->
            val updatedHistory = currentHistory.addAction(action)
            saveSwipeHistory(updatedHistory)
            updatedHistory
        }
    }

    /**
     * Update swipe preferences
     */
    fun updatePreferences(preferences: SwipePreferences) {
        _swipePreferences.value = preferences
        saveSwipePreferences(preferences)
    }

    /**
     * Get current swipe history
     */
    fun getCurrentHistory(): SwipeHistory = _swipeHistory.value

    /**
     * Get current swipe preferences
     */
    fun getCurrentPreferences(): SwipePreferences = _swipePreferences.value

    /**
     * Clear all swipe history
     */
    fun clearHistory() {
        val emptyHistory = SwipeHistory()
        _swipeHistory.value = emptyHistory
        saveSwipeHistory(emptyHistory)
    }

    /**
     * Reset preferences to default
     */
    fun resetPreferences() {
        val defaultPreferences = SwipePreferences()
        _swipePreferences.value = defaultPreferences
        saveSwipePreferences(defaultPreferences)
    }
}