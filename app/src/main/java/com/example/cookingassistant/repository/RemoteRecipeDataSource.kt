package com.example.cookingassistant.repository

import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory

interface RemoteRecipeDataSource {
    suspend fun fetchAllRecipes(): Result<List<Recipe>>

    suspend fun fetchRecipeById(id: String): Result<Recipe?>

    suspend fun searchRecipes(query: String): Result<List<Recipe>>

    suspend fun fetchRecipesByCategory(category: RecipeCategory): Result<List<Recipe>>

    suspend fun uploadRecipe(recipe: Recipe): Result<String>

    suspend fun updateRecipe(recipe: Recipe): Result<Unit>

    suspend fun deleteRecipe(id: String): Result<Unit>
}
