package com.example.cookingassistant.repository

import android.util.Log
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CachedRecipeRepository(
    private val localDataSource: LocalRecipeDataSource,
    private val remoteDataSource: RemoteRecipeDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RecipeRepository {

    companion object {
        private const val TAG = "CachedRecipeRepository"
    }

    override suspend fun getAllRecipes(): Result<List<Recipe>> = withContext(ioDispatcher) {
        Log.d(TAG, "Getting all recipes from cache")
        localDataSource.getAllRecipes()
    }

    override suspend fun getRecipeById(id: String): Result<Recipe?> = withContext(ioDispatcher) {
        Log.d(TAG, "Getting recipe by id from cache: $id")
        localDataSource.getRecipeById(id)
    }

    override suspend fun searchRecipes(query: String): Result<List<Recipe>> = withContext(ioDispatcher) {
        Log.d(TAG, "Searching recipes in cache: $query")
        localDataSource.searchRecipes(query)
    }

    override suspend fun getRecipesByCategory(category: RecipeCategory): Result<List<Recipe>> =
        withContext(ioDispatcher) {
            Log.d(TAG, "Getting recipes by category from cache: $category")
            localDataSource.getRecipesByCategory(category)
        }

    override suspend fun saveRecipe(recipe: Recipe): Result<String> = withContext(ioDispatcher) {
        Log.d(TAG, "Saving recipe: ${recipe.name}")

        val localResult = localDataSource.saveRecipe(recipe)

        // TODO: In production, sync to remote API in background
        // remoteDataSource.uploadRecipe(recipe)

        localResult
    }

    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = withContext(ioDispatcher) {
        Log.d(TAG, "Updating recipe: ${recipe.name}")

        val localResult = localDataSource.updateRecipe(recipe)

        // TODO: In production, sync to remote API in background
        // remoteDataSource.updateRecipe(recipe)

        localResult
    }

    override suspend fun deleteRecipe(id: String): Result<Unit> = withContext(ioDispatcher) {
        Log.d(TAG, "Deleting recipe: $id")

        val localResult = localDataSource.deleteRecipe(id)

        // TODO: In production, sync to remote API in background
        // remoteDataSource.deleteRecipe(id)

        localResult
    }

    suspend fun refreshRecipes(): Result<List<Recipe>> = withContext(ioDispatcher) {
        Log.d(TAG, "Refreshing recipes from remote API...")

        val allCurrentRecipes = localDataSource.getAllRecipes().getOrDefault(emptyList())
        val customRecipes = allCurrentRecipes.filter { it.isCustom }
        Log.d(TAG, "Current recipes: ${allCurrentRecipes.size} total, ${customRecipes.size} custom")
        customRecipes.forEach { recipe ->
            Log.d(TAG, "Preserving custom recipe: ${recipe.name} (id=${recipe.id})")
        }

        val remoteResult = remoteDataSource.fetchAllRecipes()

        return@withContext when {
            remoteResult.isSuccess -> {
                val freshRecipes = remoteResult.getOrThrow()
                Log.d(TAG, "Fetched ${freshRecipes.size} recipes from remote, updating cache...")

                localDataSource.saveBundledRecipes(freshRecipes)
                    .onSuccess {
                        Log.d(TAG, "Cache updated successfully with ${freshRecipes.size} bundled recipes")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to update cache with fresh recipes", error)
                    }

                val allRecipes = freshRecipes + customRecipes
                Log.d(TAG, "Returning ${allRecipes.size} total recipes (${freshRecipes.size} bundled + ${customRecipes.size} custom)")
                Result.success(allRecipes)
            }
            else -> {
                val error = remoteResult.exceptionOrNull()!!
                Log.e(TAG, "Failed to fetch recipes from remote API", error)
                Result.failure(error)
            }
        }
    }

    suspend fun getRecipeByIdWithRefresh(id: String, forceRefresh: Boolean = false): Result<Recipe?> =
        withContext(ioDispatcher) {
            if (!forceRefresh) {
                Log.d(TAG, "Getting recipe from cache: $id")
                return@withContext localDataSource.getRecipeById(id)
            }

            Log.d(TAG, "Fetching recipe from remote API: $id")
            return@withContext remoteDataSource.fetchRecipeById(id)
                .onSuccess { recipe ->
                    recipe?.let {
                        localDataSource.saveRecipe(it)
                            .onSuccess {
                                Log.d(TAG, "Recipe cached successfully: ${recipe.name}")
                            }
                            .onFailure { error ->
                                Log.e(TAG, "Failed to cache recipe", error)
                            }
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "Failed to fetch recipe from remote, falling back to cache", error)
                    return@withContext localDataSource.getRecipeById(id)
                }
        }
}
