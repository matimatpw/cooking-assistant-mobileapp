package com.example.cookingassistant.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.repository.CachedRecipeRepository
import com.example.cookingassistant.repository.FileRecipeRepository
import com.example.cookingassistant.repository.MockRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Main Glance widget for Cooking Assistant
 * Displays quick actions: Last Recipe, Continue Cooking, Random Recipe
 */
class CookingAssistantWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetState = loadWidgetState(context)

        provideContent {
            WidgetContent(
                state = widgetState,
                context = context
            )
        }
    }

    /**
     * Load widget state from preferences and repository
     */
    private suspend fun loadWidgetState(context: Context): WidgetState = withContext(Dispatchers.IO) {
        val widgetPreferences = WidgetPreferences(context)
        val repository = createRepository(context)

        // Load all recipes
        val allRecipes = repository.getAllRecipes().getOrNull() ?: emptyList()
        Log.d("CookingWidget", "Loaded ${allRecipes.size} recipes")

        if (allRecipes.isEmpty()) {
            return@withContext WidgetState(
                lastRecipe = null,
                cookingSession = null,
                randomRecipe = null,
                hasRecipes = false
            )
        }

        // Load last viewed recipe
        val lastViewedRecipeId = widgetPreferences.getLastViewedRecipe()
        Log.d("CookingWidget", "Last viewed recipe ID from prefs: $lastViewedRecipeId")
        val lastRecipe = lastViewedRecipeId?.let { recipeId ->
            val recipe = allRecipes.find { it.id == recipeId }
            if (recipe == null) {
                Log.w("CookingWidget", "Last viewed recipe not found in recipes list: $recipeId")
            }
            recipe
        }
        Log.d("CookingWidget", "Last recipe: ${lastRecipe?.name} (id: ${lastRecipe?.id})")

        // Load cooking session
        val cookingSession = widgetPreferences.getActiveCookingSession()
        val cookingRecipe = cookingSession?.let { session ->
            allRecipes.find { it.id == session.recipeId }
        }
        Log.d("CookingWidget", "Cooking session: ${cookingSession?.recipeId}")

        // Load or select random recipe
        val randomRecipe = selectRandomRecipe(widgetPreferences, allRecipes)
        Log.d("CookingWidget", "Random recipe: ${randomRecipe?.name}")

        val state = WidgetState(
            lastRecipe = lastRecipe,
            cookingSession = if (cookingRecipe != null) cookingSession else null,
            randomRecipe = randomRecipe,
            hasRecipes = true
        )

        Log.d("CookingWidget", "Widget state - hasRecipes: ${state.hasRecipes}, lastRecipe: ${state.lastRecipe != null}, randomRecipe: ${state.randomRecipe != null}, cookingSession: ${state.cookingSession != null}")

        return@withContext state
    }

    /**
     * Select random recipe for display in widget
     * Note: The actual random selection happens in MainActivity when clicked
     * This is just for showing a recipe name/preview in the widget
     */
    private fun selectRandomRecipe(
        widgetPreferences: WidgetPreferences,
        allRecipes: List<Recipe>
    ): Recipe? {
        if (allRecipes.isEmpty()) return null

        // Just pick any recipe for display (we just need to show SOMETHING)
        // The actual "random" selection happens when user clicks the button
        val randomRecipe = allRecipes.randomOrNull(Random.Default)
        Log.d("CookingWidget", "Selected random recipe for display: ${randomRecipe?.name}")
        return randomRecipe
    }

    /**
     * Create repository instance for loading recipes
     */
    private fun createRepository(context: Context): CachedRecipeRepository {
        val localDataSource = FileRecipeRepository(context)
        val remoteDataSource = MockRemoteDataSource()
        return CachedRecipeRepository(localDataSource, remoteDataSource)
    }
}

/**
 * Widget state data class
 * Contains all data needed to render the widget
 */
data class WidgetState(
    val lastRecipe: Recipe?,
    val cookingSession: CookingSession?,
    val randomRecipe: Recipe?,
    val hasRecipes: Boolean
)
