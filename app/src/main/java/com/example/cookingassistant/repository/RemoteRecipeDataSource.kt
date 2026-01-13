package com.example.cookingassistant.repository

import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory

/**
 * Interface for remote recipe data operations.
 * Implementations should handle network requests to an API.
 */
interface RemoteRecipeDataSource {
    /**
     * Fetches all recipes from the remote API
     */
    suspend fun fetchAllRecipes(): Result<List<Recipe>>

    /**
     * Fetches a specific recipe by ID from the remote API
     */
    suspend fun fetchRecipeById(id: String): Result<Recipe?>

    /**
     * Searches for recipes matching the query on the remote API
     */
    suspend fun searchRecipes(query: String): Result<List<Recipe>>

    /**
     * Fetches recipes by category from the remote API
     */
    suspend fun fetchRecipesByCategory(category: RecipeCategory): Result<List<Recipe>>

    /**
     * Uploads a new recipe to the remote API
     * @return Recipe ID assigned by the server
     */
    suspend fun uploadRecipe(recipe: Recipe): Result<String>

    /**
     * Updates an existing recipe on the remote API
     */
    suspend fun updateRecipe(recipe: Recipe): Result<Unit>

    /**
     * Deletes a recipe from the remote API
     */
    suspend fun deleteRecipe(id: String): Result<Unit>
}
