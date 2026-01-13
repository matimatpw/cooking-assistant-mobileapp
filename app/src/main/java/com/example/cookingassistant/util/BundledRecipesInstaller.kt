package com.example.cookingassistant.util

import android.content.Context
import android.util.Log
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.repository.RecipeIndex
import com.example.cookingassistant.repository.RecipeIndexEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Utility class for installing bundled recipes from assets to internal storage.
 * This simulates downloading recipes from an API on first launch.
 *
 * Bundled recipes are stored in assets/recipes/bundled/ and copied to
 * internal storage on first app launch.
 */
class BundledRecipesInstaller(private val context: Context) {

    companion object {
        private const val TAG = "BundledRecipesInstaller"
        private const val PREFS_NAME = "recipe_prefs"
        private const val KEY_BUNDLED_INSTALLED = "bundled_recipes_installed"
        private const val KEY_INSTALLATION_VERSION = "bundled_recipes_version"
        private const val CURRENT_VERSION = 1

        private const val ASSETS_RECIPES_DIR = "recipes/bundled"
        private const val INTERNAL_RECIPES_DIR = "recipes/bundled"
        private const val INTERNAL_MEDIA_DIR = "recipes/bundled/media"
    }

    /**
     * Checks if bundled recipes need to be installed and installs them if necessary.
     *
     * @return Result indicating success or failure
     */
    suspend fun installBundledRecipesIfNeeded(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isInstalled = prefs.getBoolean(KEY_BUNDLED_INSTALLED, false)
            val installedVersion = prefs.getInt(KEY_INSTALLATION_VERSION, 0)

            if (!isInstalled || installedVersion < CURRENT_VERSION) {
                Log.i(TAG, "Installing bundled recipes (version $CURRENT_VERSION)...")
                installBundledRecipes()

                prefs.edit()
                    .putBoolean(KEY_BUNDLED_INSTALLED, true)
                    .putInt(KEY_INSTALLATION_VERSION, CURRENT_VERSION)
                    .apply()

                Log.i(TAG, "Bundled recipes installed successfully")
                Result.success(Unit)
            } else {
                Log.d(TAG, "Bundled recipes already installed (version $installedVersion)")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install bundled recipes", e)
            Result.failure(e)
        }
    }

    /**
     * Installs all bundled recipes and media from assets to internal storage.
     */
    private suspend fun installBundledRecipes() {
        // Create necessary directories
        val recipesDir = File(context.filesDir, INTERNAL_RECIPES_DIR)
        val mediaDir = File(context.filesDir, INTERNAL_MEDIA_DIR)
        recipesDir.mkdirs()
        mediaDir.mkdirs()

        // Copy recipe JSON files
        copyAssetsDirectory(ASSETS_RECIPES_DIR, recipesDir)

        // Copy media files if they exist
        try {
            val mediaAssetPath = "$ASSETS_RECIPES_DIR/media"
            context.assets.list(mediaAssetPath)?.let { mediaFiles ->
                if (mediaFiles.isNotEmpty()) {
                    copyAssetsDirectory(mediaAssetPath, mediaDir)
                }
            }
        } catch (e: IOException) {
            // Media directory might not exist in assets, which is fine
            Log.d(TAG, "No media files found in assets (this is normal)")
        }

        // Build recipe index for the bundled recipes
        buildRecipeIndex()
    }

    /**
     * Recursively copies a directory from assets to internal storage.
     *
     * @param assetsPath Path in assets (e.g., "recipes/bundled")
     * @param destDir Destination directory in internal storage
     */
    private fun copyAssetsDirectory(assetsPath: String, destDir: File) {
        try {
            val files = context.assets.list(assetsPath) ?: return

            for (filename in files) {
                val assetFilePath = "$assetsPath/$filename"
                val destFile = File(destDir, filename)

                // Check if it's a directory by trying to list its contents
                val subFiles = try {
                    context.assets.list(assetFilePath)
                } catch (e: IOException) {
                    null
                }

                if (subFiles != null && subFiles.isNotEmpty()) {
                    // It's a directory, recurse
                    destFile.mkdirs()
                    copyAssetsDirectory(assetFilePath, destFile)
                } else {
                    // It's a file, copy it
                    copyAssetFile(assetFilePath, destFile)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error copying assets directory: $assetsPath", e)
            throw e
        }
    }

    /**
     * Copies a single file from assets to internal storage.
     *
     * @param assetPath Path to file in assets
     * @param destFile Destination file in internal storage
     */
    private fun copyAssetFile(assetPath: String, destFile: File) {
        try {
            context.assets.open(assetPath).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied asset: $assetPath -> ${destFile.path}")
        } catch (e: IOException) {
            Log.e(TAG, "Error copying asset file: $assetPath", e)
            throw e
        }
    }

    /**
     * Forces reinstallation of bundled recipes.
     * Useful for development/testing or when bundled recipes are updated.
     */
    suspend fun forceReinstall(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Clear installation flag
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_BUNDLED_INSTALLED, false)
                .putInt(KEY_INSTALLATION_VERSION, 0)
                .apply()

            // Delete existing bundled recipes
            val recipesDir = File(context.filesDir, INTERNAL_RECIPES_DIR)
            if (recipesDir.exists()) {
                recipesDir.deleteRecursively()
            }

            // Reinstall
            installBundledRecipesIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force reinstall bundled recipes", e)
            Result.failure(e)
        }
    }

    /**
     * Builds the recipe index by scanning bundled recipe files.
     * This index is used by FileRecipeRepository for fast recipe lookups.
     */
    private fun buildRecipeIndex() {
        try {
            val json = Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

            val recipesDir = File(context.filesDir, INTERNAL_RECIPES_DIR)
            val indexFile = File(context.filesDir, "recipes/recipes_index.json")

            // Find all recipe JSON files in the bundled directory
            val recipeFiles = recipesDir.listFiles { file ->
                file.isFile && file.extension == "json"
            } ?: emptyArray()

            Log.d(TAG, "Found ${recipeFiles.size} recipe files to index")

            // Parse each recipe file and create index entries
            val indexEntries = recipeFiles.mapNotNull { file ->
                try {
                    val recipeJson = file.readText()
                    val recipe = json.decodeFromString<Recipe>(recipeJson)

                    // Create index entry with relative path
                    val recipesBaseDir = File(context.filesDir, "recipes")
                    val relativePath = file.relativeTo(recipesBaseDir).path

                    RecipeIndexEntry(
                        id = recipe.id,
                        name = recipe.name,
                        filePath = relativePath,
                        categories = recipe.categories.toList(),
                        isCustom = recipe.isCustom,
                        thumbnail = recipe.mainPhotoUri
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse recipe file: ${file.name}", e)
                    null
                }
            }

            // Create and save the index
            val index = RecipeIndex(
                version = 1,
                lastUpdated = System.currentTimeMillis(),
                recipes = indexEntries
            )

            indexFile.writeText(json.encodeToString(index))
            Log.i(TAG, "Recipe index created with ${indexEntries.size} entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build recipe index", e)
            throw e
        }
    }
}
