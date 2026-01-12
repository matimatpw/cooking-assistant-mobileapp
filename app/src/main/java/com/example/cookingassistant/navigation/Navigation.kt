package com.example.cookingassistant.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.cookingassistant.repository.RecipeRepository
import com.example.cookingassistant.ui.theme.AddEditRecipeScreen
import com.example.cookingassistant.ui.theme.CookingStepScreen
import com.example.cookingassistant.ui.theme.RecipeDetailScreen
import com.example.cookingassistant.ui.theme.RecipeListScreen
import com.example.cookingassistant.viewmodel.RecipeViewModel

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
}

/**
 * Navigation graph for the app
 * Defines which composables are displayed for each route
 * @param navController Controls navigation between screens
 * @param repository Recipe repository for data access
 */
@Composable
fun CookingAssistantNavigation(
    navController: NavHostController,
    repository: RecipeRepository
) {
    // Single ViewModel instance shared across navigation with repository
    // This persists data as user navigates between screens
    val viewModel: RecipeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RecipeViewModel(repository) as T
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
                }
            )
        }

        // Recipe detail screen
        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            // Extract recipe ID from navigation arguments
            val recipeId = backStackEntry.arguments?.getString("recipeId")

            // Get recipe from ViewModel
            val recipe = recipeId?.let { viewModel.getRecipeById(it) }

            // Display detail screen if recipe found
            recipe?.let {
                RecipeDetailScreen(
                    recipe = it,
                    onNavigateBack = {
                        // Navigate back to list screen
                        navController.popBackStack()
                    },
                    onStartCooking = { recipeId ->
                        // Navigate to cooking step screen
                        navController.navigate(Screen.CookingStep.createRoute(recipeId))
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
                        // Navigate back to detail screen
                        navController.popBackStack()
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