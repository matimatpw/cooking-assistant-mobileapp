package com.example.cookingassistant.repository

import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory

/**
 * Interface for local recipe data operations.
 * Implementations should handle persistent storage (files, database, etc.)
 */
interface LocalRecipeDataSource {
    /**
     * Retrieves all recipes from local storage
     */
    suspend fun getAllRecipes(): Result<List<Recipe>>

    /**
     * Retrieves a specific recipe by ID from local storage
     */
    suspend fun getRecipeById(id: String): Result<Recipe?>

    /**
     * Searches for recipes matching the query in local storage
     */
    suspend fun searchRecipes(query: String): Result<List<Recipe>>

    /**
     * Retrieves recipes by category from local storage
     */
    suspend fun getRecipesByCategory(category: RecipeCategory): Result<List<Recipe>>

    /**
     * Saves multiple recipes to local storage (bulk operation)
     * These are saved as custom recipes
     */
    suspend fun saveRecipes(recipes: List<Recipe>): Result<Unit>

    /**
     * Saves bundled recipes from remote API to bundled directory
     * This is used during refresh operations
     */
    suspend fun saveBundledRecipes(recipes: List<Recipe>): Result<Unit>

    /**
     * Saves a single recipe to local storage
     * @return Recipe ID
     */
    suspend fun saveRecipe(recipe: Recipe): Result<String>

    /**
     * Updates an existing recipe in local storage
     */
    suspend fun updateRecipe(recipe: Recipe): Result<Unit>

    /**
     * Deletes a recipe from local storage
     */
    suspend fun deleteRecipe(id: String): Result<Unit>

    /**
     * Clears all recipes from local storage
     */
    suspend fun clearAllRecipes(): Result<Unit>
}
