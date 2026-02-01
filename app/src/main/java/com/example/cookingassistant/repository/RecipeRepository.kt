package com.example.cookingassistant.repository

import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory

interface RecipeRepository {

    suspend fun getAllRecipes(): Result<List<Recipe>>

    suspend fun getRecipeById(id: String): Result<Recipe?>

    suspend fun searchRecipes(query: String): Result<List<Recipe>>

    suspend fun getRecipesByCategory(category: RecipeCategory): Result<List<Recipe>>

    suspend fun saveRecipe(recipe: Recipe): Result<String>

    suspend fun updateRecipe(recipe: Recipe): Result<Unit>

    suspend fun deleteRecipe(id: String): Result<Unit>
}
