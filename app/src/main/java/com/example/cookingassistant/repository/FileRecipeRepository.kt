package com.example.cookingassistant.repository

import android.content.Context
import android.util.Log
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class FileRecipeRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val recipesDirectoryName: String = "recipes"
) : LocalRecipeDataSource {

    private val recipesDir = File(context.filesDir, recipesDirectoryName)
    private val bundledDir = File(recipesDir, "bundled")
    private val customDir = File(recipesDir, "custom")
    private val indexFile = File(recipesDir, "recipes_index.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _recipeIndex = MutableStateFlow<RecipeIndex?>(null)

    init {
        recipesDir.mkdirs()
        bundledDir.mkdirs()
        File(bundledDir, "media").mkdirs()
        customDir.mkdirs()
        File(customDir, "media").mkdirs()
    }

    override suspend fun getAllRecipes(): Result<List<Recipe>> = withContext(ioDispatcher) {
        try {
            val index = loadIndex()
            val recipes = index.recipes.mapNotNull { entry ->
                loadRecipeFromFile(File(recipesDir, entry.filePath))
            }
            Result.success(recipes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get all recipes", e)
            Result.failure(e)
        }
    }

    override suspend fun getRecipeById(id: String): Result<Recipe?> = withContext(ioDispatcher) {
        try {
            val index = loadIndex()
            val entry = index.recipes.find { it.id == id }
            val recipe = entry?.let {
                loadRecipeFromFile(File(recipesDir, it.filePath))
            }
            Result.success(recipe)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recipe by id: $id", e)
            Result.failure(e)
        }
    }

    override suspend fun searchRecipes(query: String): Result<List<Recipe>> = withContext(ioDispatcher) {
        try {
            val allRecipes = getAllRecipes().getOrThrow()
            val filtered = allRecipes.filter { recipe ->
                recipe.name.contains(query, ignoreCase = true) ||
                        recipe.description.contains(query, ignoreCase = true) ||
                        recipe.ingredients.any { ingredient ->
                            ingredient.name.contains(query, ignoreCase = true)
                        }
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search recipes with query: $query", e)
            Result.failure(e)
        }
    }

    override suspend fun getRecipesByCategory(category: RecipeCategory): Result<List<Recipe>> =
        withContext(ioDispatcher) {
            try {
                val allRecipes = getAllRecipes().getOrThrow()
                val filtered = allRecipes.filter { recipe ->
                    category in recipe.categories
                }
                Result.success(filtered)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get recipes by category: $category", e)
                Result.failure(e)
            }
        }

    override suspend fun saveRecipe(recipe: Recipe): Result<String> = withContext(ioDispatcher) {
        try {
            val recipeId = if (recipe.id.isEmpty()) {
                UUID.randomUUID().toString()
            } else {
                recipe.id
            }

            val now = System.currentTimeMillis()
            val updatedRecipe = recipe.copy(
                id = recipeId,
                updatedAt = now,
                createdAt = if (recipe.createdAt == 0L) now else recipe.createdAt,
                isCustom = true
            )

            val recipeFile = File(customDir, "recipe_$recipeId.json")

            recipeFile.writeText(json.encodeToString(updatedRecipe))

            updateIndex(updatedRecipe, recipeFile)

            Result.success(recipeId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save recipe", e)
            Result.failure(e)
        }
    }

    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> = withContext(ioDispatcher) {
        try {
            val index = loadIndex()
            val entry = index.recipes.find { it.id == recipe.id }
                ?: return@withContext Result.failure(IllegalArgumentException("Recipe with id ${recipe.id} does not exist"))

            val recipeFile = File(recipesDir, entry.filePath)

            val updatedRecipe = recipe.copy(updatedAt = System.currentTimeMillis())

            recipeFile.writeText(json.encodeToString(updatedRecipe))

            updateIndex(updatedRecipe, recipeFile)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update recipe", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteRecipe(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val index = loadIndex()
            val entry = index.recipes.find { it.id == id }

            if (entry != null) {
                val recipeFile = File(recipesDir, entry.filePath)
                if (recipeFile.exists()) {
                    recipeFile.delete()
                }

                val updatedIndex = index.copy(
                    recipes = index.recipes.filterNot { it.id == id },
                    lastUpdated = System.currentTimeMillis()
                )
                saveIndex(updatedIndex)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recipe: $id", e)
            Result.failure(e)
        }
    }

    private suspend fun loadIndex(): RecipeIndex {
        _recipeIndex.value?.let { return it }

        return if (indexFile.exists()) {
            try {
                val indexJson = indexFile.readText()
                val index = json.decodeFromString<RecipeIndex>(indexJson)
                _recipeIndex.value = index
                index
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load index, creating new one", e)
                createEmptyIndex()
            }
        } else {
            createEmptyIndex()
        }
    }

    private suspend fun createEmptyIndex(): RecipeIndex {
        val emptyIndex = RecipeIndex(
            version = 1,
            lastUpdated = System.currentTimeMillis(),
            recipes = emptyList()
        )
        saveIndex(emptyIndex)
        return emptyIndex
    }

    private suspend fun saveIndex(index: RecipeIndex) {
        try {
            indexFile.writeText(json.encodeToString(index))
            _recipeIndex.value = index
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save index", e)
            throw e
        }
    }

    private suspend fun updateIndex(recipe: Recipe, recipeFile: File) {
        val index = loadIndex()

        val relativePath = recipeFile.relativeTo(recipesDir).path
        val indexEntry = RecipeIndexEntry(
            id = recipe.id,
            name = recipe.name,
            filePath = relativePath,
            categories = recipe.categories.toList(),
            isCustom = recipe.isCustom,
            thumbnail = recipe.mainPhotoUri
        )

        val updatedRecipes = index.recipes.filterNot { it.id == recipe.id } + indexEntry

        val updatedIndex = index.copy(
            recipes = updatedRecipes,
            lastUpdated = System.currentTimeMillis()
        )
        saveIndex(updatedIndex)
    }

    override suspend fun saveRecipes(recipes: List<Recipe>): Result<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Saving ${recipes.size} recipes to local storage...")
            recipes.forEach { recipe ->
                saveRecipe(recipe)
            }
            Log.d(TAG, "Successfully saved ${recipes.size} recipes")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save recipes batch", e)
            Result.failure(e)
        }
    }

    override suspend fun saveBundledRecipes(recipes: List<Recipe>): Result<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Saving ${recipes.size} bundled recipes from API...")

            val currentIndex = loadIndex()
            val customRecipeEntries = currentIndex.recipes.filter { it.isCustom }
            Log.d(TAG, "Preserving ${customRecipeEntries.size} custom recipe entries in index")

            bundledDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension == "json") {
                    file.delete()
                    Log.d(TAG, "Deleted old bundled recipe file: ${file.name}")
                }
            }

            val newBundledEntries = mutableListOf<RecipeIndexEntry>()

            recipes.forEachIndexed { index, recipe ->
                val now = System.currentTimeMillis()

                val bundledRecipe = recipe.copy(
                    id = recipe.id.ifEmpty { (index + 1).toString().padStart(3, '0') },
                    updatedAt = now,
                    createdAt = if (recipe.createdAt == 0L) now else recipe.createdAt,
                    isCustom = false
                )

                val recipeFile = File(bundledDir, "recipe_${bundledRecipe.id}.json")
                recipeFile.writeText(json.encodeToString(bundledRecipe))
                Log.d(TAG, "Saved bundled recipe: ${bundledRecipe.name}")

                val relativePath = recipeFile.relativeTo(recipesDir).path
                val indexEntry = RecipeIndexEntry(
                    id = bundledRecipe.id,
                    name = bundledRecipe.name,
                    filePath = relativePath,
                    categories = bundledRecipe.categories.toList(),
                    isCustom = false,
                    thumbnail = bundledRecipe.mainPhotoUri
                )
                newBundledEntries.add(indexEntry)
            }

            val updatedIndex = currentIndex.copy(
                recipes = newBundledEntries + customRecipeEntries,
                lastUpdated = System.currentTimeMillis()
            )
            saveIndex(updatedIndex)

            Log.d(TAG, "Successfully saved ${recipes.size} bundled recipes, index now has ${updatedIndex.recipes.size} total entries")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bundled recipes", e)
            Result.failure(e)
        }
    }

    override suspend fun clearAllRecipes(): Result<Unit> = withContext(ioDispatcher) {
        try {
            Log.d(TAG, "Clearing all recipes from local storage...")

            bundledDir.deleteRecursively()
            customDir.deleteRecursively()

            bundledDir.mkdirs()
            customDir.mkdirs()
            File(bundledDir, "media").mkdirs()
            File(customDir, "media").mkdirs()

            createEmptyIndex()

            Log.d(TAG, "All recipes cleared successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all recipes", e)
            Result.failure(e)
        }
    }

    private fun loadRecipeFromFile(file: File): Recipe? {
        return try {
            if (!file.exists()) return null
            val recipeJson = file.readText()
            json.decodeFromString<Recipe>(recipeJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load recipe from ${file.path}", e)
            null
        }
    }

    companion object {
        private const val TAG = "FileRecipeRepository"
    }
}

@Serializable
data class RecipeIndex(
    val version: Int,
    val lastUpdated: Long,
    val recipes: List<RecipeIndexEntry>
)

@Serializable
data class RecipeIndexEntry(
    val id: String,
    val name: String,
    val filePath: String,
    val categories: List<RecipeCategory>,
    val isCustom: Boolean,
    val thumbnail: String? = null
)
