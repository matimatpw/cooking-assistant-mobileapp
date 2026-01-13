package com.example.cookingassistant.repository

import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory

/**
 * Repository interface for recipe data operations.
 * Provides abstraction layer between data sources and the ViewModel.
 *
 * All methods return Result<T> to handle success/failure cases gracefully.
 * All methods are suspend functions for coroutine support.
 */
interface RecipeRepository {

    /**
     * Retrieves all recipes (both bundled and custom).
     *
     * @return Result containing list of all recipes, or failure with exception
     */
    suspend fun getAllRecipes(): Result<List<Recipe>>

    /**
     * Retrieves a single recipe by its ID.
     *
     * @param id The unique identifier of the recipe
     * @return Result containing the recipe if found, null if not found, or failure with exception
     */
    suspend fun getRecipeById(id: String): Result<Recipe?>

    /**
     * Searches recipes by query string.
     * Searches in recipe name, description, and ingredient names.
     *
     * @param query Search query string
     * @return Result containing list of matching recipes, or failure with exception
     */
    suspend fun searchRecipes(query: String): Result<List<Recipe>>

    /**
     * Retrieves recipes that belong to a specific category.
     *
     * @param category The category to filter by
     * @return Result containing list of recipes in that category, or failure with exception
     */
    suspend fun getRecipesByCategory(category: RecipeCategory): Result<List<Recipe>>

    /**
     * Saves a new recipe or creates a copy with a new ID.
     * If the recipe has an empty ID, a new UUID will be generated.
     *
     * @param recipe The recipe to save
     * @return Result containing the recipe ID (newly generated or existing), or failure with exception
     */
    suspend fun saveRecipe(recipe: Recipe): Result<String>

    /**
     * Updates an existing recipe.
     * The recipe must have a valid ID that exists in the repository.
     *
     * @param recipe The recipe to update with modified fields
     * @return Result indicating success (Unit) or failure with exception
     */
    suspend fun updateRecipe(recipe: Recipe): Result<Unit>

    /**
     * Deletes a recipe and all its associated media files.
     *
     * @param id The ID of the recipe to delete
     * @return Result indicating success (Unit) or failure with exception
     */
    suspend fun deleteRecipe(id: String): Result<Unit>
}
