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

/**
 * Sealed class defining all navigation routes in the app
 * Helps prevent typos and provides type-safe navigation
 */
sealed class Screen(val route: String) {
    object RecipeList : Screen("recipe_list")
    object RecipeDetail : Screen("recipe_detail/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe_detail/$recipeId"
    }
    object CookingStep : Screen("cooking_step/{recipeId}") {
        fun createRoute(recipeId: String) = "cooking_step/$recipeId"
    }
    object AddRecipe : Screen("add_recipe")
    object EditRecipe : Screen("edit_recipe/{recipeId}") {
        fun createRoute(recipeId: String) = "edit_recipe/$recipeId"
    }
    object SwipeModule: Screen("swipe")
    object LikedRecipes: Screen("liked_recipes")
}

/**
 * Navigation graph for the app
 * Defines which composables are displayed for each route
 * @param navController Controls navigation between screens
 * @param repository Cached recipe repository for data access
 * @param context Application context for widget preferences
 */
@Composable
fun CookingAssistantNavigation(
    navController: NavHostController,
    repository: CachedRecipeRepository,
    context: Context
) {
    // Widget preferences for tracking last viewed recipe and cooking session
    val widgetPreferences = WidgetPreferences(context)

    val swipeRepository = SwipeRepository(context)

    // Single ViewModel instance shared across navigation with repository and widget preferences
    // This persists data as user navigates between screens
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
        // Recipe list screen (home screen)
        composable(route = Screen.RecipeList.route) {
            RecipeListScreen(
                viewModel = viewModel,
                onRecipeClick = { recipeId ->
                    // Navigate to detail screen with recipe ID
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                },
                onAddRecipe = {
                    // Navigate to add recipe screen
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
            LikedRecipesScreen(  // Use the new screen I created
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

        // Last recipe handler (for widget deep link)
        // This route reads the last viewed recipe from preferences and navigates to it
        composable(
            route = "last_recipe",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "cookingassistant://last_recipe"
                }
            )
        ) {
            // Read last viewed recipe from preferences
            val lastRecipeId = widgetPreferences.getLastViewedRecipe()
            val recipes = viewModel.recipes.value
            val lastRecipe = lastRecipeId?.let { id -> recipes.find { it.id == id } }

            // Log for debugging
            android.util.Log.d("Navigation", "Last recipe handler: lastRecipeId=$lastRecipeId, found=${lastRecipe?.name}")

            // Navigate to the last recipe detail
            LaunchedEffect(Unit) {
                if (lastRecipe != null) {
                    navController.navigate(Screen.RecipeDetail.createRoute(lastRecipe.id)) {
                        // Pop the last_recipe route from back stack so back button works correctly
                        popUpTo("last_recipe") { inclusive = true }
                    }
                } else {
                    // No last recipe available, go to recipe list
                    android.util.Log.w("Navigation", "No last viewed recipe found")
                    navController.navigate(Screen.RecipeList.route) {
                        popUpTo("last_recipe") { inclusive = true }
                    }
                }
            }
        }

        // Random recipe handler (for widget deep link)
        // This route picks a random recipe and navigates to it
        composable(
            route = "random_recipe",
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "cookingassistant://random_recipe"
                }
            )
        ) {
            // Pick a random recipe from current recipes
            val recipes = viewModel.recipes.value
            val randomRecipe = recipes.randomOrNull()

            // Log for debugging
            android.util.Log.d("Navigation", "Random recipe handler: picked ${randomRecipe?.name} from ${recipes.size} recipes")

            // Navigate to the random recipe detail
            LaunchedEffect(Unit) {
                if (randomRecipe != null) {
                    navController.navigate(Screen.RecipeDetail.createRoute(randomRecipe.id)) {
                        // Pop the random_recipe route from back stack so back button works correctly
                        popUpTo("random_recipe") { inclusive = true }
                    }
                } else {
                    // No recipes available, go to recipe list
                    android.util.Log.w("Navigation", "No recipes available for random selection")
                    navController.navigate(Screen.RecipeList.route) {
                        popUpTo("random_recipe") { inclusive = true }
                    }
                }
            }
        }

        // Recipe detail screen
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
            // Extract recipe ID from navigation arguments
            val recipeId = backStackEntry.arguments?.getString("recipeId")

            // Track last viewed recipe for widget
            LaunchedEffect(recipeId) {
                android.util.Log.d("Navigation", "LaunchedEffect triggered for recipeId: $recipeId")
                recipeId?.let {
                    android.util.Log.d("Navigation", "About to save last viewed recipe: $it")
                    widgetPreferences.setLastViewedRecipe(it)
                    android.util.Log.d("Navigation", "About to trigger widget update")
                    // Trigger widget refresh to show updated last recipe
                    WidgetUpdater.updateWidgets(context)
                    android.util.Log.d("Navigation", "Widget update triggered")
                }
            }

            // Get recipe from ViewModel
            val recipe = recipeId?.let { viewModel.getRecipeById(it) }

            // Display detail screen if recipe found
            recipe?.let {
                RecipeDetailScreen(
                    recipe = it,
                    onNavigateBack = {
                        // Navigate back - if no back stack, go to list screen
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.RecipeList.route) {
                                popUpTo(Screen.RecipeList.route) { inclusive = true }
                            }
                        }
                    },
                    onStartCooking = { recipeId ->
                        // Navigate to cooking step screen
                        navController.navigate(Screen.CookingStep.createRoute(recipeId))
                    },
                    onEdit = { recipeId ->
                        // Navigate to edit recipe screen
                        navController.navigate(Screen.EditRecipe.createRoute(recipeId))
                    }
                )
            }
        }

        // Cooking step screen
        composable(
            route = Screen.CookingStep.route,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.StringType
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "cookingassistant://cooking_step/{recipeId}"
                }
            )
        ) { backStackEntry ->
            // Extract recipe ID from navigation arguments
            val recipeId = backStackEntry.arguments?.getString("recipeId")

            // Get recipe from ViewModel
            val recipe = recipeId?.let { viewModel.getRecipeById(it) }

            // Display cooking step screen if recipe found
            recipe?.let {
                CookingStepScreen(
                    recipe = it,
                    viewModel = viewModel,
                    onNavigateBack = {
                        // Navigate back - if no back stack, go to list screen
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.RecipeList.route) {
                                popUpTo(Screen.RecipeList.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        // Add recipe screen
        composable(route = Screen.AddRecipe.route) {
            AddEditRecipeScreen(
                recipeId = null,
                viewModel = viewModel,
                onSave = {
                    // Navigate back to list screen after saving
                    navController.popBackStack()
                },
                onCancel = {
                    // Navigate back without saving
                    navController.popBackStack()
                }
            )
        }

        // Edit recipe screen
        composable(
            route = Screen.EditRecipe.route,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            // Extract recipe ID from navigation arguments
            val recipeId = backStackEntry.arguments?.getString("recipeId")

            AddEditRecipeScreen(
                recipeId = recipeId,
                viewModel = viewModel,
                onSave = {
                    // Navigate back to list screen after saving
                    navController.popBackStack()
                },
                onCancel = {
                    // Navigate back without saving
                    navController.popBackStack()
                }
            )
        }
    }
}