package com.example.cookingassistant.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
        fun createRoute(recipeId: Int) = "recipe_detail/$recipeId"
    }
}

/**
 * Navigation graph for the app
 * Defines which composables are displayed for each route
 * @param navController Controls navigation between screens
 */
@Composable
fun CookingAssistantNavigation(navController: NavHostController) {
    // Single ViewModel instance shared across navigation
    // This persists data as user navigates between screens
    val viewModel: RecipeViewModel = viewModel()

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
                }
            )
        }

        // Recipe detail screen
        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            // Extract recipe ID from navigation arguments
            val recipeId = backStackEntry.arguments?.getInt("recipeId")

            // Get recipe from ViewModel
            val recipe = recipeId?.let { viewModel.getRecipeById(it) }

            // Display detail screen if recipe found
            recipe?.let {
                RecipeDetailScreen(
                    recipe = it,
                    onNavigateBack = {
                        // Navigate back to list screen
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}