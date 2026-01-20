package com.example.cookingassistant.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.example.cookingassistant.MainActivity

/**
 * Sealed class defining all possible widget actions
 * Provides type-safe action handling for widget buttons
 */
sealed class WidgetAction(val uriPattern: String) {
    /**
     * Open recipe detail screen
     */
    data class OpenRecipe(val recipeId: String) : WidgetAction("cookingassistant://recipe_detail/$recipeId")

    /**
     * Continue cooking session at saved step
     */
    data class ContinueCooking(val recipeId: String, val stepIndex: Int) :
        WidgetAction("cookingassistant://cooking_step/$recipeId?step=$stepIndex")

    /**
     * Open last viewed recipe (reads from preferences at click time)
     */
    object OpenLastRecipe : WidgetAction("cookingassistant://last_recipe")

    /**
     * Open random recipe (picks new random recipe on click)
     */
    object OpenRandomRecipe : WidgetAction("cookingassistant://random_recipe")

    /**
     * Open main app (recipe list)
     */
    object OpenApp : WidgetAction("cookingassistant://recipe_list")
}

/**
 * Widget action builders for Glance
 * Provides helper functions to create clickable actions for widget buttons
 */
object WidgetActionBuilder {

    /**
     * Create action to open a specific recipe
     */
    fun openRecipeAction(context: Context, recipeId: String): Action {
        return actionStartActivity(
            createDeepLinkIntent(context, "cookingassistant://recipe_detail/$recipeId")
        )
    }

    /**
     * Create action to open last viewed recipe
     * Uses special deep link that reads last recipe from preferences at click time
     */
    fun openLastRecipeAction(context: Context): Action {
        return actionStartActivity(
            createDeepLinkIntent(context, "cookingassistant://last_recipe")
        )
    }

    /**
     * Create action to continue cooking session
     * Uses special deep link that finds the recipe with active timers at click time
     */
    fun continueCookingAction(context: Context, recipeId: String, stepIndex: Int = 0): Action {
        return actionStartActivity(
            createDeepLinkIntent(context, "cookingassistant://continue_cooking")
        )
    }

    /**
     * Create action to open random recipe
     * Uses special deep link that picks a random recipe at click time
     */
    fun openRandomRecipeAction(context: Context): Action {
        return actionStartActivity(
            createDeepLinkIntent(context, "cookingassistant://random_recipe")
        )
    }

    /**
     * Create action to open main app
     */
    fun openAppAction(context: Context): Action {
        return actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )
    }

    /**
     * Create deep link intent for navigation
     */
    private fun createDeepLinkIntent(context: Context, uri: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setClass(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}
