package com.example.cookingassistant.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

@Composable
fun WidgetContent(
    state: WidgetState,
    context: Context
) {
    val size = LocalSize.current

    android.util.Log.d("CookingWidget", "Widget size: ${size.width} x ${size.height}")

    MediumWidgetLayout(state, context)
}

@Composable
fun SmallWidgetLayout(
    state: WidgetState,
    context: Context
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF5F5F5)))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!state.hasRecipes) {
            NoRecipesContent(context)
        } else {
            when {
                state.cookingSession != null -> {
                    ActionButton(
                        text = "Continue Cooking",
                        onClick = WidgetActionBuilder.continueCookingAction(
                            context,
                            state.cookingSession.recipeId,
                            state.cookingSession.stepIndex
                        )
                    )
                }
                state.lastRecipe != null -> {
                    ActionButton(
                        text = "Last Recipe",
                        onClick = WidgetActionBuilder.openLastRecipeAction(context)
                    )
                }
                state.randomRecipe != null -> {
                    ActionButton(
                        text = "Random Recipe",
                        onClick = WidgetActionBuilder.openRandomRecipeAction(context)
                    )
                }
            }
        }
    }
}

@Composable
fun MediumWidgetLayout(
    state: WidgetState,
    context: Context
) {
    android.util.Log.d("CookingWidget", "MediumWidgetLayout - hasRecipes: ${state.hasRecipes}, lastRecipe: ${state.lastRecipe != null}, randomRecipe: ${state.randomRecipe != null}, cookingSession: ${state.cookingSession != null}")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF5F5F5)))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!state.hasRecipes) {
            android.util.Log.d("CookingWidget", "Showing NoRecipesContent")
            NoRecipesContent(context)
        } else {
            Text(
                text = "Cooking Assistant",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF6200EE)),
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            if (state.cookingSession != null) {
                android.util.Log.d("CookingWidget", "Adding Continue Cooking button")
                ActionButton(
                    text = "Continue Cooking",
                    onClick = WidgetActionBuilder.continueCookingAction(
                        context,
                        state.cookingSession.recipeId,
                        state.cookingSession.stepIndex
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            if (state.lastRecipe != null) {
                android.util.Log.d("CookingWidget", "Adding Last Recipe button: ${state.lastRecipe.name}")
                ActionButton(
                    text = "Last Recipe",
                    onClick = WidgetActionBuilder.openLastRecipeAction(context)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            if (state.randomRecipe != null) {
                android.util.Log.d("CookingWidget", "Adding Random Recipe button: ${state.randomRecipe.name}")
                ActionButton(
                    text = "Random Recipe",
                    onClick = WidgetActionBuilder.openRandomRecipeAction(context)
                )
            }
        }
    }
}
@Composable
fun LargeWidgetLayout(
    state: WidgetState,
    context: Context
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFF5F5F5)))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!state.hasRecipes) {
            NoRecipesContent(context)
        } else {
            Text(
                text = "Cooking Assistant",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF6200EE)),
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            val previewRecipe = when {
                state.cookingSession != null -> {
                    state.cookingSession.recipeId.let { recipeId ->
                        state.lastRecipe?.takeIf { it.id == recipeId }
                    }
                }
                else -> state.lastRecipe
            }

            previewRecipe?.let { recipe ->
                Text(
                    text = recipe.name,
                    style = TextStyle(
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "${recipe.cookingTime} min · ${recipe.difficulty.name}",
                    style = TextStyle(
                        color = ColorProvider(Color.Gray)
                    )
                )
                Spacer(modifier = GlanceModifier.height(12.dp))
            }
            if (state.cookingSession != null) {
                ActionButton(
                    text = "Continue Cooking",
                    onClick = WidgetActionBuilder.continueCookingAction(
                        context,
                        state.cookingSession.recipeId,
                        state.cookingSession.stepIndex
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            if (state.lastRecipe != null) {
                ActionButton(
                    text = "Last Recipe",
                    onClick = WidgetActionBuilder.openLastRecipeAction(context)
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
            }

            if (state.randomRecipe != null) {
                ActionButton(
                    text = "Random Recipe",
                    onClick = WidgetActionBuilder.openRandomRecipeAction(context)
                )
            }
        }
    }
}

@Composable
fun NoRecipesContent(context: Context) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Add your first recipe!",
            style = TextStyle(
                color = ColorProvider(Color.Gray)
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        ActionButton(
            text = "Open App",
            onClick = WidgetActionBuilder.openAppAction(context)
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    onClick: androidx.glance.action.Action
) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(40.dp)
            .background(ColorProvider(Color(0xFF6200EE)))
            .cornerRadius(8.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontWeight = FontWeight.Medium
            )
        )
    }
}
