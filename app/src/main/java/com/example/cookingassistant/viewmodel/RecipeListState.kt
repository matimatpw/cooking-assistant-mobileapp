package com.example.cookingassistant.viewmodel

import com.example.cookingassistant.model.Recipe

/**
 * Sealed class representing different states of the recipe list.
 * Used for reactive UI updates with proper loading, success, and error states.
 */
sealed class RecipeListState {
    /**
     * Initial loading state when app starts and no cached data exists yet
     */
    object Loading : RecipeListState()

    /**
     * Success state with recipe data
     * @param recipes List of recipes to display
     * @param isRefreshing True when pull-to-refresh is active (shows loading indicator)
     */
    data class Success(
        val recipes: List<Recipe>,
        val isRefreshing: Boolean = false
    ) : RecipeListState()

    /**
     * Error state when something goes wrong
     * @param message Error message to display
     * @param cachedRecipes Previously cached recipes to show (if available)
     */
    data class Error(
        val message: String,
        val cachedRecipes: List<Recipe> = emptyList()
    ) : RecipeListState()
}
