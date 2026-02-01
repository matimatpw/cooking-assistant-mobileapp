package com.example.cookingassistant.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.cookingassistant.repository.CachedRecipeRepository
import com.example.cookingassistant.ui.theme.AddEditRecipeScreen
import com.example.cookingassistant.ui.theme.CookingStepScreen
import com.example.cookingassistant.ui.theme.RecipeDetailScreen
import com.example.cookingassistant.ui.theme.RecipeListScreen
import com.example.cookingassistant.viewmodel.RecipeViewModel
import com.example.cookingassistant.widget.WidgetPreferences
import com.example.cookingassistant.widget.WidgetUpdater
import com.example.cookingassistant.repository.SwipeRepository
import com.example.cookingassistant.ui.theme.LikedRecipesScreen
import com.example.cookingassistant.ui.theme.SwipeScreen

sealed class Screen(val route: String) {
    object RecipeList : Screen("recipe_list")
    object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe_detail/$recipeId"
    }
    object CookingStep : Screen("cooking_step/{recipeId}?stepIndex={stepIndex}") {
        fun createRoute(recipeId: String, stepIndex: Int = 0) = "cooking_step/$recipeId?stepIndex=$stepIndex"
    }
    object AddRecipe : Screen("add_recipe")
    object EditRecipe : Screen("edit_recipe/{recipeId}") {
        fun createRoute(recipeId: String) = "edit_recipe/$recipeId"
    }
    object SwipeModule: Screen("swipe")
    object LikedRecipes: Screen("liked_recipes")
}

@Composable
fun CookingAssistantNavigation(
    navController: NavHostController,
    repository: CachedRecipeRepository,
    context: Context
) {
    val widgetPreferences = WidgetPreferences(context)

    val swipeRepository = SwipeRepository(context)

    val viewModel: RecipeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RecipeViewModel(repository, widgetPreferences, context) as T
            }
        }
    )

    NavHost(
        navController = navController,
        startDestination = Screen.RecipeList.route
    ) {
        composable(route = Screen.RecipeList.route) {
            RecipeListScreen(
                viewModel = viewModel,
                onRecipeClick = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                },
                onAddRecipe = {
                    navController.navigate(Screen.AddRecipe.route)
                },
                onNavigateToSwipe = {
                    navController.navigate(Screen.SwipeModule.route)
                }
            )
        }

        composable(route = Screen.SwipeModule.route) {
            SwipeScreen(
                recipeRepository = repository,
                swipeRepository = swipeRepository,
                onNavigateToLikedRecipes = {
                    navController.navigate(Screen.LikedRecipes.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.RecipeList.route)
                }
            )
        }

        composable(route = Screen.LikedRecipes.route) {
            LikedRecipesScreen(
                viewModel = viewModel,
                swipeRepository = swipeRepository,
                onRecipeClick = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "last_recipe",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "cookingassistant://last_recipe"
                }
            )
        ) {
            val lastRecipeId = widgetPreferences.getLastViewedRecipe()
            val recipes = viewModel.recipes.value
            val lastRecipe = lastRecipeId?.let { id -> recipes.find { it.id == id } }

            android.util.Log.d("Navigation", "Last recipe handler: lastRecipeId=$lastRecipeId, found=${lastRecipe?.name}")

            LaunchedEffect(Unit) {
                if (lastRecipe != null) {
                    navController.navigate(Screen.RecipeDetail.createRoute(lastRecipe.id)) {
                        popUpTo("last_recipe") { inclusive = true }
                    }
                } else {
                    android.util.Log.w("Navigation", "No last viewed recipe found")
                    navController.navigate(Screen.RecipeList.route) {
                        popUpTo("last_recipe") { inclusive = true }
                    }
                }
            }
        }

        composable(
            route = "random_recipe",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "cookingassistant://random_recipe"
                }
            )
        ) {
            val recipes = viewModel.recipes.value
            val randomRecipe = recipes.randomOrNull()

            android.util.Log.d("Navigation", "Random recipe handler: picked ${randomRecipe?.name} from ${recipes.size} recipes")

            LaunchedEffect(Unit) {
                if (randomRecipe != null) {
                    navController.navigate(Screen.RecipeDetail.createRoute(randomRecipe.id)) {
                        popUpTo("random_recipe") { inclusive = true }
                    }
                } else {
                    android.util.Log.w("Navigation", "No recipes available for random selection")
                    navController.navigate(Screen.RecipeList.route) {
                        popUpTo("random_recipe") { inclusive = true }
                    }
                }
            }
        }

        composable(
            route = "continue_cooking",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "cookingassistant://continue_cooking"
                }
            )
        ) {
            val activeTimerRecipeId = widgetPreferences.getActiveTimerRecipe()
            val cookingSession = widgetPreferences.getActiveCookingSession()

            val targetRecipeId = activeTimerRecipeId ?: cookingSession?.recipeId
            val targetStepIndex = if (activeTimerRecipeId != null) 0 else (cookingSession?.stepIndex ?: 0)

            val recipes = viewModel.recipes.value
            val targetRecipe = targetRecipeId?.let { id -> recipes.find { it.id == id } }

            android.util.Log.d("Navigation", "Continue cooking: activeTimerRecipe=$activeTimerRecipeId, cookingSession=${cookingSession?.recipeId}, targetRecipe=${targetRecipe?.name}")

            LaunchedEffect(Unit) {
                if (targetRecipe != null) {
                    navController.navigate(Screen.CookingStep.createRoute(targetRecipe.id, targetStepIndex)) {
                        popUpTo("continue_cooking") { inclusive = true }
                    }
                } else {
                    android.util.Log.w("Navigation", "No active cooking session found")
                    navController.navigate(Screen.RecipeList.route) {
                        popUpTo("continue_cooking") { inclusive = true }
                    }
                }
            }
        }

        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "cookingassistant://recipe_detail/{recipeId}"
                }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")

            LaunchedEffect(recipeId) {
                android.util.Log.d("Navigation", "LaunchedEffect triggered for recipeId: $recipeId")
                recipeId?.let {
                    android.util.Log.d("Navigation", "About to save last viewed recipe: $it")
                    widgetPreferences.setLastViewedRecipe(it)
                    android.util.Log.d("Navigation", "About to trigger widget update")
                    WidgetUpdater.updateWidgets(context)
                    android.util.Log.d("Navigation", "Widget update triggered")
                }
            }

            val recipe = recipeId?.let { viewModel.getRecipeById(it) }

            recipe?.let {
                RecipeDetailScreen(
                    recipe = it,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.RecipeList.route) {
                                popUpTo(Screen.RecipeList.route) { inclusive = true }
                            }
                        }
                    },
                    onStartCooking = { recipeId ->
                        navController.navigate(Screen.CookingStep.createRoute(recipeId))
                    },
                    onEdit = { recipeId ->
                        navController.navigate(Screen.EditRecipe.createRoute(recipeId))
                    }
                )
            }
        }

        composable(
            route = Screen.CookingStep.route,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.StringType
                },
                navArgument("stepIndex") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "cookingassistant://cooking_step/{recipeId}?stepIndex={stepIndex}"
                },
                navDeepLink {
                    uriPattern = "cookingassistant://cooking_step/{recipeId}"
                }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")
            val initialStepIndex = backStackEntry.arguments?.getInt("stepIndex") ?: 0

            val recipe = recipeId?.let { viewModel.getRecipeById(it) }

            recipe?.let {
                CookingStepScreen(
                    recipe = it,
                    viewModel = viewModel,
                    initialStepIndex = initialStepIndex,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.RecipeList.route) {
                                popUpTo(Screen.RecipeList.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        composable(route = Screen.AddRecipe.route) {
            AddEditRecipeScreen(
                recipeId = null,
                viewModel = viewModel,
                onSave = {
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditRecipe.route,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")

            AddEditRecipeScreen(
                recipeId = recipeId,
                viewModel = viewModel,
                onSave = {
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}