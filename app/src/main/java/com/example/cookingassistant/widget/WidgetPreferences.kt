package com.example.cookingassistant.widget

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun setLastViewedRecipe(recipeId: String) {
        android.util.Log.d("WidgetPreferences", "Setting last viewed recipe: $recipeId")
        prefs.edit()
            .putString(KEY_LAST_VIEWED_RECIPE_ID, recipeId)
            .putLong(KEY_LAST_VIEWED_TIMESTAMP, System.currentTimeMillis())
            .apply()
        android.util.Log.d("WidgetPreferences", "Last viewed recipe saved successfully")
    }

    fun getLastViewedRecipe(): String? {
        return prefs.getString(KEY_LAST_VIEWED_RECIPE_ID, null)
    }

    fun setActiveCookingSession(recipeId: String, stepIndex: Int) {
        prefs.edit()
            .putString(KEY_ACTIVE_COOKING_RECIPE_ID, recipeId)
            .putInt(KEY_ACTIVE_COOKING_STEP_INDEX, stepIndex)
            .putLong(KEY_ACTIVE_COOKING_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getActiveCookingSession(): CookingSession? {
        val recipeId = prefs.getString(KEY_ACTIVE_COOKING_RECIPE_ID, null) ?: return null
        val stepIndex = prefs.getInt(KEY_ACTIVE_COOKING_STEP_INDEX, 0)
        val timestamp = prefs.getLong(KEY_ACTIVE_COOKING_TIMESTAMP, 0)

        val currentTime = System.currentTimeMillis()
        val sessionAge = currentTime - timestamp
        if (sessionAge > SESSION_STALE_THRESHOLD) {
            clearCookingSession()
            return null
        }

        return CookingSession(recipeId, stepIndex, timestamp)
    }

    fun clearCookingSession() {
        prefs.edit()
            .remove(KEY_ACTIVE_COOKING_RECIPE_ID)
            .remove(KEY_ACTIVE_COOKING_STEP_INDEX)
            .remove(KEY_ACTIVE_COOKING_TIMESTAMP)
            .apply()
    }

    fun setActiveTimerRecipe(recipeId: String) {
        android.util.Log.d("WidgetPreferences", "Setting active timer recipe: $recipeId")
        prefs.edit()
            .putString(KEY_ACTIVE_TIMER_RECIPE_ID, recipeId)
            .apply()
    }

    fun getActiveTimerRecipe(): String? {
        return prefs.getString(KEY_ACTIVE_TIMER_RECIPE_ID, null)
    }

    fun clearActiveTimerRecipe() {
        android.util.Log.d("WidgetPreferences", "Clearing active timer recipe")
        prefs.edit()
            .remove(KEY_ACTIVE_TIMER_RECIPE_ID)
            .apply()
    }

    fun hasActiveCookingSession(): Boolean {
        return getActiveCookingSession() != null
    }

    fun setCachedRandomRecipe(recipeId: String) {
        prefs.edit()
            .putString(KEY_CACHED_RANDOM_RECIPE_ID, recipeId)
            .putLong(KEY_CACHED_RANDOM_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }
    fun getCachedRandomRecipe(): String? {
        val recipeId = prefs.getString(KEY_CACHED_RANDOM_RECIPE_ID, null) ?: return null
        val timestamp = prefs.getLong(KEY_CACHED_RANDOM_TIMESTAMP, 0)

        val currentTime = System.currentTimeMillis()
        val cacheAge = currentTime - timestamp
        if (cacheAge > RANDOM_CACHE_DURATION) {
            return null
        }

        return recipeId
    }

    companion object {
        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_LAST_VIEWED_RECIPE_ID = "last_viewed_recipe_id"
        private const val KEY_LAST_VIEWED_TIMESTAMP = "last_viewed_timestamp"
        private const val KEY_ACTIVE_COOKING_RECIPE_ID = "active_cooking_recipe_id"
        private const val KEY_ACTIVE_COOKING_STEP_INDEX = "active_cooking_step_index"
        private const val KEY_ACTIVE_COOKING_TIMESTAMP = "active_cooking_timestamp"
        private const val KEY_CACHED_RANDOM_RECIPE_ID = "cached_random_recipe_id"
        private const val KEY_CACHED_RANDOM_TIMESTAMP = "cached_random_timestamp"
        private const val KEY_ACTIVE_TIMER_RECIPE_ID = "active_timer_recipe_id"

        private const val SESSION_STALE_THRESHOLD = 24 * 60 * 60 * 1000L

        private const val RANDOM_CACHE_DURATION = 5 * 60 * 1000L
    }
}

@Serializable
data class CookingSession(
    val recipeId: String,
    val stepIndex: Int,
    val timestamp: Long
)
