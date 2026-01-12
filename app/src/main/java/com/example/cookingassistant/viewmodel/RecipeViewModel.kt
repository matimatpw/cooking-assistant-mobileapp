package com.example.cookingassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingassistant.model.Difficulty
import com.example.cookingassistant.model.Ingredient
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory
import com.example.cookingassistant.model.RecipeStep
import com.example.cookingassistant.repository.RecipeRepository
import com.example.cookingassistant.voice.VoiceCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing recipe data and step navigation
 * Follows MVVM pattern - exposes state via StateFlow, no UI references
 */
class RecipeViewModel(
    private val repository: RecipeRepository? = null
) : ViewModel() {

    // Private mutable state - only ViewModel can modify
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())

    // Public immutable state - UI observes this
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    // Current step index for cooking mode
    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    // Currently active recipe in cooking mode
    private val _activeRecipe = MutableStateFlow<Recipe?>(null)
    val activeRecipe: StateFlow<Recipe?> = _activeRecipe.asStateFlow()

    init {
        // Load recipes from repository or use hardcoded fallback
        loadRecipes()
    }

    /**
     * Loads recipe data from repository or falls back to hardcoded data
     */
    private fun loadRecipes() {
        if (repository != null) {
            // Load from repository
            viewModelScope.launch {
                repository.getAllRecipes()
                    .onSuccess { recipes ->
                        _recipes.value = recipes
                    }
                    .onFailure { error ->
                        // Log error and fall back to hardcoded recipes
                        android.util.Log.e("RecipeViewModel", "Failed to load recipes from repository", error)
                        loadHardcodedRecipes()
                    }
            }
        } else {
            // Fall back to hardcoded recipes
            loadHardcodedRecipes()
        }
    }

    /**
     * Loads hardcoded recipe data into the state
     * Used as fallback or for testing
     */
    private fun loadHardcodedRecipes() {
        val now = System.currentTimeMillis()
        _recipes.value = listOf(
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

    /**
     * Finds a recipe by its ID
     * @param id The recipe ID to search for
     * @return Recipe object or null if not found
     */
    fun getRecipeById(id: String): Recipe? {
        return _recipes.value.find { it.id == id }
    }

    /**
     * Start cooking mode for a recipe
     * @param recipe The recipe to start cooking
     */
    fun startCookingMode(recipe: Recipe) {
        _activeRecipe.value = recipe
        _currentStepIndex.value = 0
    }

    /**
     * Navigate to the next step
     * @return true if navigation successful, false if already at last step
     */
    fun nextStep(): Boolean {
        val recipe = _activeRecipe.value ?: return false
        val currentIndex = _currentStepIndex.value
        val maxIndex = recipe.steps.size - 1

        return if (currentIndex < maxIndex) {
            _currentStepIndex.value = currentIndex + 1
            true
        } else {
            false
        }
    }

    /**
     * Navigate to the previous step
     * @return true if navigation successful, false if already at first step
     */
    fun previousStep(): Boolean {
        val currentIndex = _currentStepIndex.value

        return if (currentIndex > 0) {
            _currentStepIndex.value = currentIndex - 1
            true
        } else {
            false
        }
    }

    /**
     * Go to a specific step
     * @param stepIndex The step index to navigate to
     */
    fun goToStep(stepIndex: Int) {
        val recipe = _activeRecipe.value ?: return
        val maxIndex = recipe.steps.size - 1

        if (stepIndex in 0..maxIndex) {
            _currentStepIndex.value = stepIndex
        }
    }

    /**
     * Reset to first step
     */
    fun resetToFirstStep() {
        _currentStepIndex.value = 0
    }

    /**
     * Process voice command
     * @param command The voice command to process
     */
    fun processVoiceCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.NEXT -> nextStep()
            VoiceCommand.PREVIOUS -> previousStep()
            VoiceCommand.REPEAT -> {
                // Repeat is handled by staying on current step
                // Could potentially trigger TTS to read current step again
            }
            VoiceCommand.START -> resetToFirstStep()
        }
    }

    /**
     * Exit cooking mode
     */
    fun exitCookingMode() {
        _activeRecipe.value = null
        _currentStepIndex.value = 0
    }

    /**
     * Save a new recipe
     * @param recipe The recipe to save
     */
    fun saveRecipe(recipe: Recipe) {
        if (repository != null) {
            viewModelScope.launch {
                repository.saveRecipe(recipe)
                    .onSuccess { recipeId ->
                        android.util.Log.d("RecipeViewModel", "Recipe saved with ID: $recipeId")
                        // Reload recipes to include the new one
                        loadRecipes()
                    }
                    .onFailure { error ->
                        android.util.Log.e("RecipeViewModel", "Failed to save recipe", error)
                    }
            }
        } else {
            android.util.Log.w("RecipeViewModel", "Cannot save recipe: repository is null")
        }
    }

    /**
     * Update an existing recipe
     * @param recipe The recipe to update
     */
    fun updateRecipe(recipe: Recipe) {
        if (repository != null) {
            viewModelScope.launch {
                repository.updateRecipe(recipe)
                    .onSuccess {
                        android.util.Log.d("RecipeViewModel", "Recipe updated: ${recipe.id}")
                        // Reload recipes to reflect changes
                        loadRecipes()
                    }
                    .onFailure { error ->
                        android.util.Log.e("RecipeViewModel", "Failed to update recipe", error)
                    }
            }
        } else {
            android.util.Log.w("RecipeViewModel", "Cannot update recipe: repository is null")
        }
    }

    /**
     * Delete a recipe
     * @param recipeId The ID of the recipe to delete
     */
    fun deleteRecipe(recipeId: String) {
        if (repository != null) {
            viewModelScope.launch {
                repository.deleteRecipe(recipeId)
                    .onSuccess {
                        android.util.Log.d("RecipeViewModel", "Recipe deleted: $recipeId")
                        // Reload recipes to remove the deleted one
                        loadRecipes()
                    }
                    .onFailure { error ->
                        android.util.Log.e("RecipeViewModel", "Failed to delete recipe", error)
                    }
            }
        } else {
            android.util.Log.w("RecipeViewModel", "Cannot delete recipe: repository is null")
        }
    }
}
