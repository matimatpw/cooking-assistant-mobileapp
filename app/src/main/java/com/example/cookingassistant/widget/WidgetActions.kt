package com.example.cookingassistant.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import com.example.cookingassistant.MainActivity

sealed class WidgetAction(val uriPattern: String) {
    data class OpenRecipe(val recipeId: String) : WidgetAction("cookingassistant://recipe_detail/$recipeId")

    data class ContinueCooking(val recipeId: String, val stepIndex: Int) :
        WidgetAction("cookingassistant://cooking_step/$recipeId?step=$stepIndex")

    object OpenLastRecipe : WidgetAction("cookingassistant://last_recipe")

    object OpenRandomRecipe : WidgetAction("cookingassistant://random_recipe")

    object OpenApp : WidgetAction("cookingassistant://recipe_list")
}

object WidgetActionBuilder {

    fun openRecipeAction(context: Context, recipeId: String): Action {
        return actionStartActivity(
            createDeepLinkIntent(context, "cookingassistant://recipe_detail/$recipeId")
        )
    }

    fun openLastRecipeAction(context: Context): Action {
        return actionStartActivity(
            createDeepLinkIntent(context, "cookingassistant://last_recipe")
        )
    }

    fun continueCookingAction(context: Context, recipeId: String, stepIndex: Int = 0): Action {
        return actionStartActivity(
            createDeepLinkIntent(context, "cookingassistant://continue_cooking")
        )
    }

    fun openRandomRecipeAction(context: Context): Action {
        return actionStartActivity(
            createDeepLinkIntent(context, "cookingassistant://random_recipe")
        )
    }

    fun openAppAction(context: Context): Action {
        return actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        )
    }

    private fun createDeepLinkIntent(context: Context, uri: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setClass(context, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}
