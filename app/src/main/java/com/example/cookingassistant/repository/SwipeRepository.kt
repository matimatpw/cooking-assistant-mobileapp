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

    private fun saveSwipeHistory(history: SwipeHistory) {
        try {
            val jsonString = json.encodeToString(history)
            swipeHistoryFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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

    private fun saveSwipePreferences(preferences: SwipePreferences) {
        try {
            val jsonString = json.encodeToString(preferences)
            swipePreferencesFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recordSwipe(action: SwipeAction) {
        _swipeHistory.update { currentHistory ->
            val updatedHistory = currentHistory.addAction(action)
            saveSwipeHistory(updatedHistory)
            updatedHistory
        }
    }

    fun updatePreferences(preferences: SwipePreferences) {
        _swipePreferences.value = preferences
        saveSwipePreferences(preferences)
    }

    fun getCurrentHistory(): SwipeHistory = _swipeHistory.value

    fun getCurrentPreferences(): SwipePreferences = _swipePreferences.value

    fun clearHistory() {
        val emptyHistory = SwipeHistory()
        _swipeHistory.value = emptyHistory
        saveSwipeHistory(emptyHistory)
    }

    fun resetPreferences() {
        val defaultPreferences = SwipePreferences()
        _swipePreferences.value = defaultPreferences
        saveSwipePreferences(defaultPreferences)
    }
}