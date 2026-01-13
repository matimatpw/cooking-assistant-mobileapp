package com.example.cookingassistant.repository

import android.util.Log
import com.example.cookingassistant.model.Difficulty
import com.example.cookingassistant.model.Ingredient
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory
import com.example.cookingassistant.model.RecipeStep
import kotlinx.coroutines.delay

/**
 * Mock implementation of RemoteRecipeDataSource for development and testing.
 * Simulates network latency and returns hardcoded recipes.
 *
 * This will be replaced with a real API client in production.
 */
class MockRemoteDataSource(
    private val networkDelayMs: Long = 1000
) : RemoteRecipeDataSource {

    companion object {
        private const val TAG = "MockRemoteDataSource"
    }

    override suspend fun fetchAllRecipes(): Result<List<Recipe>> {
        Log.d(TAG, "Fetching all recipes from mock API...")
        delay(networkDelayMs) // Simulate network latency

        return try {
            val recipes = generateMockRecipes()
            Log.d(TAG, "Successfully fetched ${recipes.size} recipes from mock API")
            Result.success(recipes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch recipes from mock API", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchRecipeById(id: String): Result<Recipe?> {
        Log.d(TAG, "Fetching recipe with id=$id from mock API...")
        delay(networkDelayMs)

        return try {
            val recipe = generateMockRecipes().find { it.id == id }
            Log.d(TAG, if (recipe != null) "Found recipe: ${recipe.name}" else "Recipe not found")
            Result.success(recipe)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch recipe by id", e)
            Result.failure(e)
        }
    }

    override suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        Log.d(TAG, "Searching recipes with query='$query' on mock API...")
        delay(networkDelayMs)

        return try {
            val allRecipes = generateMockRecipes()
            val filtered = allRecipes.filter { recipe ->
                recipe.name.contains(query, ignoreCase = true) ||
                        recipe.description.contains(query, ignoreCase = true) ||
                        recipe.ingredients.any { ingredient ->
                            ingredient.name.contains(query, ignoreCase = true)
                        }
            }
            Log.d(TAG, "Search returned ${filtered.size} recipes")
            Result.success(filtered)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search recipes", e)
            Result.failure(e)
        }
    }

    override suspend fun fetchRecipesByCategory(category: RecipeCategory): Result<List<Recipe>> {
        Log.d(TAG, "Fetching recipes by category=$category from mock API...")
        delay(networkDelayMs)

        return try {
            val allRecipes = generateMockRecipes()
            val filtered = allRecipes.filter { recipe ->
                category in recipe.categories
            }
            Log.d(TAG, "Found ${filtered.size} recipes in category $category")
            Result.success(filtered)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch recipes by category", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadRecipe(recipe: Recipe): Result<String> {
        Log.d(TAG, "Uploading recipe '${recipe.name}' to mock API...")
        delay(networkDelayMs)

        return try {
            // In real implementation, this would POST to API and return server-assigned ID
            val recipeId = recipe.id.ifEmpty { "mock_${System.currentTimeMillis()}" }
            Log.d(TAG, "Recipe uploaded successfully with id=$recipeId")
            Result.success(recipeId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload recipe", e)
            Result.failure(e)
        }
    }

    override suspend fun updateRecipe(recipe: Recipe): Result<Unit> {
        Log.d(TAG, "Updating recipe '${recipe.name}' on mock API...")
        delay(networkDelayMs)

        return try {
            // In real implementation, this would PUT/PATCH to API
            Log.d(TAG, "Recipe updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update recipe", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteRecipe(id: String): Result<Unit> {
        Log.d(TAG, "Deleting recipe with id=$id from mock API...")
        delay(networkDelayMs)

        return try {
            // In real implementation, this would DELETE from API
            Log.d(TAG, "Recipe deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recipe", e)
            Result.failure(e)
        }
    }

    /**
     * Generates mock recipe data that simulates what would come from an API.
     * These are the same recipes as bundled recipes to maintain consistency.
     */
    private fun generateMockRecipes(): List<Recipe> {
        val now = System.currentTimeMillis()
        return listOf(
            Recipe(
                id = "001",
                name = "Spaghetti Carbonara",
                description = "Classic Italian pasta with creamy egg sauce and crispy pancetta",
                mainPhotoUri = null,
                ingredients = listOf(
                    Ingredient("spaghetti", "400g"),
                    Ingredient("pancetta or guanciale", "200g", "cut into small cubes"),
                    Ingredient("eggs", "4 large"),
                    Ingredient("Pecorino Romano cheese", "100g", "grated"),
                    Ingredient("black pepper", "to taste"),
                    Ingredient("salt", "to taste")
                ),
                steps = listOf(
                    RecipeStep(1, "Bring a large pot of salted water to boil and cook spaghetti according to package directions.", 10),
                    RecipeStep(2, "While pasta cooks, cut pancetta into small cubes and fry in a large pan until crispy.", 5),
                    RecipeStep(3, "In a bowl, whisk together eggs, grated cheese, and generous black pepper.", 2),
                    RecipeStep(4, "Reserve 1 cup of pasta water, then drain the spaghetti.", 1),
                    RecipeStep(5, "Remove pan from heat, add drained pasta to the pancetta.", 1),
                    RecipeStep(6, "Quickly pour in the egg mixture, tossing constantly. Add pasta water to create a creamy sauce.", 2),
                    RecipeStep(7, "Serve immediately with extra cheese and black pepper.", 1)
                ),
                cookingTime = 20,
                prepTime = 10,
                servings = 4,
                difficulty = Difficulty.MEDIUM,
                categories = setOf(RecipeCategory.ITALIAN, RecipeCategory.LUNCH, RecipeCategory.DINNER),
                tags = listOf("pasta", "italian", "classic"),
                createdAt = now,
                updatedAt = now,
                isCustom = false
            ),
            Recipe(
                id = "002",
                name = "Chicken Stir Fry",
                description = "Quick and healthy Asian-inspired stir fry with chicken and vegetables",
                mainPhotoUri = null,
                ingredients = listOf(
                    Ingredient("chicken breast", "500g", "sliced"),
                    Ingredient("bell peppers", "2", "sliced"),
                    Ingredient("onion", "1", "sliced"),
                    Ingredient("garlic", "3 cloves", "minced"),
                    Ingredient("soy sauce", "3 tbsp"),
                    Ingredient("vegetable oil", "2 tbsp"),
                    Ingredient("cornstarch", "1 tbsp"),
                    Ingredient("cooked rice", "for serving")
                ),
                steps = listOf(
                    RecipeStep(1, "Mix chicken with 1 tbsp soy sauce and cornstarch. Let marinate for 10 minutes.", 10),
                    RecipeStep(2, "Heat 1 tbsp oil in a wok or large pan over high heat.", 2),
                    RecipeStep(3, "Add chicken and stir-fry until golden brown, about 5 minutes. Remove and set aside.", 5),
                    RecipeStep(4, "Add remaining oil to the pan. Stir-fry garlic for 30 seconds.", 1),
                    RecipeStep(5, "Add peppers and onion, stir-fry for 3-4 minutes until slightly softened.", 4),
                    RecipeStep(6, "Return chicken to pan, add remaining soy sauce.", 1),
                    RecipeStep(7, "Toss everything together for 1-2 minutes.", 2),
                    RecipeStep(8, "Serve hot over cooked rice.")
                ),
                cookingTime = 25,
                prepTime = 15,
                servings = 4,
                difficulty = Difficulty.EASY,
                categories = setOf(RecipeCategory.ASIAN, RecipeCategory.DINNER, RecipeCategory.QUICK_MEAL),
                tags = listOf("chicken", "stir-fry", "asian", "quick"),
                createdAt = now,
                updatedAt = now,
                isCustom = false
            ),
            Recipe(
                id = "003",
                name = "Chocolate Chip Cookies",
                description = "Classic homemade chocolate chip cookies - crispy edges with soft centers",
                mainPhotoUri = null,
                ingredients = listOf(
                    Ingredient("all-purpose flour", "2 1/4 cups"),
                    Ingredient("baking soda", "1 tsp"),
                    Ingredient("salt", "1 tsp"),
                    Ingredient("butter", "1 cup", "softened"),
                    Ingredient("granulated sugar", "3/4 cup"),
                    Ingredient("brown sugar", "3/4 cup"),
                    Ingredient("eggs", "2 large"),
                    Ingredient("vanilla extract", "2 tsp"),
                    Ingredient("chocolate chips", "2 cups")
                ),
                steps = listOf(
                    RecipeStep(1, "Preheat oven to 375°F (190°C).", 5),
                    RecipeStep(2, "Mix flour, baking soda, and salt in a bowl.", 2),
                    RecipeStep(3, "In another bowl, beat butter and both sugars until creamy.", 3),
                    RecipeStep(4, "Add eggs and vanilla to the butter mixture, beat well.", 2),
                    RecipeStep(5, "Gradually blend in the flour mixture.", 2),
                    RecipeStep(6, "Stir in chocolate chips.", 1),
                    RecipeStep(7, "Drop rounded tablespoons of dough onto ungreased cookie sheets.", 3),
                    RecipeStep(8, "Bake for 9-11 minutes or until golden brown.", 10),
                    RecipeStep(9, "Cool on baking sheet for 2 minutes, then transfer to a wire rack.", 2)
                ),
                cookingTime = 30,
                prepTime = 15,
                servings = 24,
                difficulty = Difficulty.EASY,
                categories = setOf(RecipeCategory.DESSERT, RecipeCategory.AMERICAN, RecipeCategory.PARTY),
                tags = listOf("cookies", "dessert", "baking", "chocolate"),
                createdAt = now,
                updatedAt = now,
                isCustom = false
            ),
            Recipe(
                id = "004",
                name = "Greek Salad",
                description = "Fresh and healthy Mediterranean salad with feta cheese and olives",
                mainPhotoUri = null,
                ingredients = listOf(
                    Ingredient("tomatoes", "4", "cut into wedges"),
                    Ingredient("cucumber", "1", "sliced"),
                    Ingredient("red onion", "1", "thinly sliced"),
                    Ingredient("green bell pepper", "1", "sliced"),
                    Ingredient("feta cheese", "200g", "cubed"),
                    Ingredient("Kalamata olives", "1/2 cup"),
                    Ingredient("olive oil", "3 tbsp"),
                    Ingredient("red wine vinegar", "1 tbsp"),
                    Ingredient("dried oregano", "1 tsp"),
                    Ingredient("salt and pepper", "to taste")
                ),
                steps = listOf(
                    RecipeStep(1, "Wash and prepare all vegetables as indicated.", 5),
                    RecipeStep(2, "In a large bowl, combine tomatoes, cucumber, onion, and bell pepper.", 2),
                    RecipeStep(3, "In a small bowl, whisk together olive oil, vinegar, oregano, salt, and pepper.", 2),
                    RecipeStep(4, "Pour dressing over the vegetables and toss gently.", 1),
                    RecipeStep(5, "Top with feta cheese and olives.", 1),
                    RecipeStep(6, "Serve immediately or refrigerate for up to 2 hours before serving.", 1)
                ),
                cookingTime = 15,
                prepTime = 10,
                servings = 4,
                difficulty = Difficulty.EASY,
                categories = setOf(RecipeCategory.GREEK, RecipeCategory.LUNCH, RecipeCategory.VEGETARIAN, RecipeCategory.QUICK_MEAL),
                tags = listOf("salad", "greek", "healthy", "vegetarian"),
                createdAt = now,
                updatedAt = now,
                isCustom = false
            ),
            Recipe(
                id = "005",
                name = "Banana Smoothie",
                description = "Creamy and refreshing banana smoothie - perfect for breakfast",
                mainPhotoUri = null,
                ingredients = listOf(
                    Ingredient("ripe bananas", "2"),
                    Ingredient("milk", "1 cup", "dairy or plant-based"),
                    Ingredient("Greek yogurt", "1/2 cup"),
                    Ingredient("honey", "1 tbsp"),
                    Ingredient("vanilla extract", "1/2 tsp"),
                    Ingredient("ice cubes", "1 cup"),
                    Ingredient("cinnamon", "pinch", "optional")
                ),
                steps = listOf(
                    RecipeStep(1, "Peel bananas and break into chunks.", 1),
                    RecipeStep(2, "Add all ingredients to a blender.", 1),
                    RecipeStep(3, "Blend on high speed until smooth and creamy, about 30-60 seconds.", 1),
                    RecipeStep(4, "Taste and adjust sweetness with more honey if desired.", 1),
                    RecipeStep(5, "Pour into glasses and serve immediately.", 1),
                    RecipeStep(6, "Optional: sprinkle with cinnamon on top.")
                ),
                cookingTime = 5,
                prepTime = 5,
                servings = 2,
                difficulty = Difficulty.EASY,
                categories = setOf(RecipeCategory.BREAKFAST, RecipeCategory.QUICK_MEAL, RecipeCategory.VEGETARIAN),
                tags = listOf("smoothie", "breakfast", "banana", "quick", "healthy"),
                createdAt = now,
                updatedAt = now,
                isCustom = false
            )
        )
    }
}
