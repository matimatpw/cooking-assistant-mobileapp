package com.example.cookingassistant.viewmodel

import androidx.lifecycle.ViewModel
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.voice.VoiceCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for managing recipe data and step navigation
 * Follows MVVM pattern - exposes state via StateFlow, no UI references
 */
class RecipeViewModel : ViewModel() {

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
        // Load hardcoded recipes when ViewModel is created
        loadRecipes()
    }

    /**
     * Loads hardcoded recipe data into the state
     * In a real app, this would fetch from a repository/database
     */
    private fun loadRecipes() {
        _recipes.value = listOf(
            Recipe(
                id = 1,
                name = "Spaghetti Carbonara",
                ingredients = listOf(
                    "400g spaghetti",
                    "200g pancetta or guanciale",
                    "4 large eggs",
                    "100g Pecorino Romano cheese",
                    "Black pepper",
                    "Salt"
                ),
                instructions = listOf(
                    "Bring a large pot of salted water to boil and cook spaghetti according to package directions.",
                    "While pasta cooks, cut pancetta into small cubes and fry in a large pan until crispy.",
                    "In a bowl, whisk together eggs, grated cheese, and generous black pepper.",
                    "Reserve 1 cup of pasta water, then drain the spaghetti.",
                    "Remove pan from heat, add drained pasta to the pancetta.",
                    "Quickly pour in the egg mixture, tossing constantly. Add pasta water to create a creamy sauce.",
                    "Serve immediately with extra cheese and black pepper."
                ),
                cookingTime = 20
            ),
            Recipe(
                id = 2,
                name = "Chicken Stir Fry",
                ingredients = listOf(
                    "500g chicken breast, sliced",
                    "2 bell peppers, sliced",
                    "1 onion, sliced",
                    "3 cloves garlic, minced",
                    "3 tbsp soy sauce",
                    "2 tbsp vegetable oil",
                    "1 tbsp cornstarch",
                    "Cooked rice for serving"
                ),
                instructions = listOf(
                    "Mix chicken with 1 tbsp soy sauce and cornstarch. Let marinate for 10 minutes.",
                    "Heat 1 tbsp oil in a wok or large pan over high heat.",
                    "Add chicken and stir-fry until golden brown, about 5 minutes. Remove and set aside.",
                    "Add remaining oil to the pan. Stir-fry garlic for 30 seconds.",
                    "Add peppers and onion, stir-fry for 3-4 minutes until slightly softened.",
                    "Return chicken to pan, add remaining soy sauce.",
                    "Toss everything together for 1-2 minutes.",
                    "Serve hot over cooked rice."
                ),
                cookingTime = 25
            ),
            Recipe(
                id = 3,
                name = "Chocolate Chip Cookies",
                ingredients = listOf(
                    "2 1/4 cups all-purpose flour",
                    "1 tsp baking soda",
                    "1 tsp salt",
                    "1 cup butter, softened",
                    "3/4 cup granulated sugar",
                    "3/4 cup brown sugar",
                    "2 large eggs",
                    "2 tsp vanilla extract",
                    "2 cups chocolate chips"
                ),
                instructions = listOf(
                    "Preheat oven to 375°F (190°C).",
                    "Mix flour, baking soda, and salt in a bowl.",
                    "In another bowl, beat butter and both sugars until creamy.",
                    "Add eggs and vanilla to the butter mixture, beat well.",
                    "Gradually blend in the flour mixture.",
                    "Stir in chocolate chips.",
                    "Drop rounded tablespoons of dough onto ungreased cookie sheets.",
                    "Bake for 9-11 minutes or until golden brown.",
                    "Cool on baking sheet for 2 minutes, then transfer to a wire rack."
                ),
                cookingTime = 30
            ),
            Recipe(
                id = 4,
                name = "Greek Salad",
                ingredients = listOf(
                    "4 tomatoes, cut into wedges",
                    "1 cucumber, sliced",
                    "1 red onion, thinly sliced",
                    "1 green bell pepper, sliced",
                    "200g feta cheese, cubed",
                    "1/2 cup Kalamata olives",
                    "3 tbsp olive oil",
                    "1 tbsp red wine vinegar",
                    "1 tsp dried oregano",
                    "Salt and pepper"
                ),
                instructions = listOf(
                    "Wash and prepare all vegetables as indicated.",
                    "In a large bowl, combine tomatoes, cucumber, onion, and bell pepper.",
                    "In a small bowl, whisk together olive oil, vinegar, oregano, salt, and pepper.",
                    "Pour dressing over the vegetables and toss gently.",
                    "Top with feta cheese and olives.",
                    "Serve immediately or refrigerate for up to 2 hours before serving."
                ),
                cookingTime = 15
            ),
            Recipe(
                id = 5,
                name = "Banana Smoothie",
                ingredients = listOf(
                    "2 ripe bananas",
                    "1 cup milk (dairy or plant-based)",
                    "1/2 cup Greek yogurt",
                    "1 tbsp honey",
                    "1/2 tsp vanilla extract",
                    "1 cup ice cubes",
                    "Optional: pinch of cinnamon"
                ),
                instructions = listOf(
                    "Peel bananas and break into chunks.",
                    "Add all ingredients to a blender.",
                    "Blend on high speed until smooth and creamy, about 30-60 seconds.",
                    "Taste and adjust sweetness with more honey if desired.",
                    "Pour into glasses and serve immediately.",
                    "Optional: sprinkle with cinnamon on top."
                ),
                cookingTime = 5
            )
        )
    }

    /**
     * Finds a recipe by its ID
     * @param id The recipe ID to search for
     * @return Recipe object or null if not found
     */
    fun getRecipeById(id: Int): Recipe? {
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
        val maxIndex = recipe.instructions.size - 1

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
        val maxIndex = recipe.instructions.size - 1

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
}
