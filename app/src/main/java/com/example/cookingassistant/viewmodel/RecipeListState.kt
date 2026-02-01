package com.example.cookingassistant.viewmodel

import com.example.cookingassistant.model.Recipe

sealed class RecipeListState {
    object Loading : RecipeListState()

    data class Success(
        val recipes: List<Recipe>,
        val isRefreshing: Boolean = false
    ) : RecipeListState()

    data class Error(
        val message: String,
        val cachedRecipes: List<Recipe> = emptyList()
    ) : RecipeListState()
}
