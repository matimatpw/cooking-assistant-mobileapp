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

    private suspend fun loadWidgetState(context: Context): WidgetState = withContext(Dispatchers.IO) {
        val widgetPreferences = WidgetPreferences(context)
        val repository = createRepository(context)

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

        val activeTimerRecipeId = widgetPreferences.getActiveTimerRecipe()
        val activeTimerRecipe = activeTimerRecipeId?.let { id ->
            allRecipes.find { it.id == id }
        }
        Log.d("CookingWidget", "Active timer recipe: $activeTimerRecipeId (found: ${activeTimerRecipe?.name})")

        val cookingSession = widgetPreferences.getActiveCookingSession()
        val cookingRecipe = if (activeTimerRecipe != null) {
            activeTimerRecipe
        } else {
            cookingSession?.let { session ->
                allRecipes.find { it.id == session.recipeId }
            }
        }
        Log.d("CookingWidget", "Cooking session: ${cookingSession?.recipeId}, using recipe: ${cookingRecipe?.name}")

        val randomRecipe = selectRandomRecipe(widgetPreferences, allRecipes)
        Log.d("CookingWidget", "Random recipe: ${randomRecipe?.name}")

        val effectiveCookingSession = when {
            activeTimerRecipe != null -> CookingSession(activeTimerRecipe.id, 0, System.currentTimeMillis())
            cookingRecipe != null && cookingSession != null -> cookingSession
            else -> null
        }

        val state = WidgetState(
            lastRecipe = lastRecipe,
            cookingSession = effectiveCookingSession,
            randomRecipe = randomRecipe,
            hasRecipes = true
        )

        Log.d("CookingWidget", "Widget state - hasRecipes: ${state.hasRecipes}, lastRecipe: ${state.lastRecipe != null}, randomRecipe: ${state.randomRecipe != null}, cookingSession: ${state.cookingSession != null}")

        return@withContext state
    }

    private fun selectRandomRecipe(
        widgetPreferences: WidgetPreferences,
        allRecipes: List<Recipe>
    ): Recipe? {
        if (allRecipes.isEmpty()) return null

        val randomRecipe = allRecipes.randomOrNull(Random.Default)
        Log.d("CookingWidget", "Selected random recipe for display: ${randomRecipe?.name}")
        return randomRecipe
    }

    private fun createRepository(context: Context): CachedRecipeRepository {
        val localDataSource = FileRecipeRepository(context)
        val remoteDataSource = MockRemoteDataSource()
        return CachedRecipeRepository(localDataSource, remoteDataSource)
    }
}

data class WidgetState(
    val lastRecipe: Recipe?,
    val cookingSession: CookingSession?,
    val randomRecipe: Recipe?,
    val hasRecipes: Boolean
)
