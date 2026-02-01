package com.example.cookingassistant.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingassistant.model.Difficulty
import com.example.cookingassistant.model.Ingredient
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory
import com.example.cookingassistant.model.RecipeDraft
import com.example.cookingassistant.model.RecipeFilters
import com.example.cookingassistant.model.RecipeStep
import com.example.cookingassistant.repository.CachedRecipeRepository
import com.example.cookingassistant.model.TimerState
import com.example.cookingassistant.model.TimerStatus
import com.example.cookingassistant.timer.TimerAlarmCallback
import com.example.cookingassistant.timer.TimerServiceBridge
import com.example.cookingassistant.voice.VoiceCommand
import com.example.cookingassistant.widget.WidgetPreferences
import com.example.cookingassistant.widget.WidgetUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

interface TtsCallback {
    fun speakInstruction(instruction: String)
    fun speakIngredients(ingredients: List<Ingredient>)
    fun speakDuration(minutes: Int)
    fun speakTips(tips: String)
    fun speakStepNumber(stepNumber: Int, totalSteps: Int)

    fun speakTimerStarted(minutes: Int)
    fun speakTimerPaused(minutes: Int, seconds: Int)
    fun speakTimerResumed()
    fun speakTimerCancelled()
    fun speakTimerFinished(stepNumber: Int)
    fun speakTimerStatus(minutes: Int, seconds: Int)
    fun speakMessage(message: String)
}

class RecipeViewModel(
    private val repository: CachedRecipeRepository? = null,
    private val widgetPreferences: WidgetPreferences? = null,
    private val context: android.content.Context? = null
) : ViewModel() {

    companion object {
        private const val TAG = "RecipeViewModel"
    }

    private val _state = MutableStateFlow<RecipeListState>(RecipeListState.Loading)
    val state: StateFlow<RecipeListState> = _state.asStateFlow()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _activeRecipe = MutableStateFlow<Recipe?>(null)
    val activeRecipe: StateFlow<Recipe?> = _activeRecipe.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    private val _recipeDraft = MutableStateFlow<RecipeDraft?>(null)
    val recipeDraft: StateFlow<RecipeDraft?> = _recipeDraft.asStateFlow()

    private val _filters = MutableStateFlow(RecipeFilters())
    val filters: StateFlow<RecipeFilters> = _filters.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private var ttsCallback: TtsCallback? = null

    private val _timers = MutableStateFlow<Map<Int, TimerState>>(emptyMap())
    val timers: StateFlow<Map<Int, TimerState>> = _timers.asStateFlow()

    val currentStepTimer: StateFlow<TimerState?> = combine(
        _currentStepIndex,
        _timers
    ) { stepIndex, timersMap ->
        timersMap[stepIndex]
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var timerServiceBridge: TimerServiceBridge? = null

    private var timerAlarmCallback: TimerAlarmCallback? = null

    init {
        loadRecipes()
    }

    fun loadRecipes() {
        if (repository != null) {
            viewModelScope.launch {
                Log.d(TAG, "Loading recipes from cache...")
                _state.value = RecipeListState.Loading

                repository.getAllRecipes()
                    .onSuccess { recipes ->
                        Log.d(TAG, "Loaded ${recipes.size} recipes from cache")
                        _recipes.value = recipes
                        _state.value = RecipeListState.Success(recipes)
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to load recipes from cache", error)
                        loadHardcodedRecipes()
                        _state.value = RecipeListState.Success(_recipes.value)
                    }
            }
        } else {
            Log.w(TAG, "No repository provided, using hardcoded recipes")
            loadHardcodedRecipes()
            _state.value = RecipeListState.Success(_recipes.value)
        }
    }

    fun refreshRecipes() {
        if (repository != null) {
            viewModelScope.launch {
                Log.d(TAG, "Refreshing recipes from remote API...")

                val currentRecipes = when (val currentState = _state.value) {
                    is RecipeListState.Success -> currentState.recipes
                    is RecipeListState.Error -> currentState.cachedRecipes
                    else -> _recipes.value
                }
                _state.value = RecipeListState.Success(currentRecipes, isRefreshing = true)

                repository.refreshRecipes()
                    .onSuccess { freshRecipes ->
                        Log.d(TAG, "Successfully refreshed ${freshRecipes.size} recipes")
                        _recipes.value = freshRecipes
                        _state.value = RecipeListState.Success(freshRecipes, isRefreshing = false)
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to refresh recipes", error)
                        _state.value = RecipeListState.Error(
                            message = error.message ?: "Failed to refresh recipes",
                            cachedRecipes = currentRecipes
                        )
                    }
            }
        } else {
            Log.w(TAG, "No repository provided, cannot refresh recipes")
            _state.value = RecipeListState.Error(
                message = "Cannot refresh: No repository",
                cachedRecipes = _recipes.value
            )
        }
    }

    fun selectTab(index: Int) {
        require(index in 0..2) { "Invalid tab index: $index. Must be 0 or 1 or 2." }
        _selectedTabIndex.value = index
    }

    fun getRecipesForTab(tabIndex: Int, recipes: List<Recipe>): List<Recipe> {
        val tabFiltered = when (tabIndex) {
            0 -> recipes.filter { !it.isCustom } // Explore: bundled recipes
            1 -> recipes.filter { it.isCustom }  // My Recipes: custom recipes
            2 -> recipes.filter { !it.isCustom } // Swipe: bundled recipes
            else -> recipes // Fallback: show all
        }

        return applyFilters(tabFiltered, _filters.value)
    }

    fun applyFilters(recipes: List<Recipe>, filters: RecipeFilters): List<Recipe> {
        if (!filters.hasActiveFilters()) {
            return recipes
        }

        return recipes.filter { recipe ->
            val matchesMealType = filters.mealTypes.isEmpty() ||
                    filters.mealTypes.any { it in recipe.categories }

            val matchesDietary = filters.dietaryPreferences.isEmpty() ||
                    filters.dietaryPreferences.all { it in recipe.categories }

            val matchesCuisine = filters.cuisines.isEmpty() ||
                    filters.cuisines.any { it in recipe.categories }

            val matchesDifficulty = filters.difficulties.isEmpty() ||
                    recipe.difficulty in filters.difficulties

            val matchesCookingTime = (filters.minCookingTime == null || recipe.cookingTime >= filters.minCookingTime) &&
                    (filters.maxCookingTime == null || recipe.cookingTime <= filters.maxCookingTime)

            matchesMealType && matchesDietary && matchesCuisine && matchesDifficulty && matchesCookingTime
        }
    }

    fun updateFilters(filters: RecipeFilters) {
        _filters.value = filters
    }
    fun clearFilters() {
        _filters.value = RecipeFilters()
    }
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
                    RecipeStep(
                        stepNumber = 1,
                        instruction = "Bring a large pot of salted water to boil and cook spaghetti according to package directions.",
                        durationMinutes = 10,
                        ingredients = listOf(
                            Ingredient("spaghetti", "400g"),
                            Ingredient("salt", "to taste")
                        )
                    ),
                    RecipeStep(
                        stepNumber = 2,
                        instruction = "While pasta cooks, cut pancetta into small cubes and fry in a large pan until crispy.",
                        durationMinutes = 5,
                        ingredients = listOf(
                            Ingredient("pancetta or guanciale", "200g", "cut into small cubes")
                        )
                    ),
                    RecipeStep(
                        stepNumber = 3,
                        instruction = "In a bowl, whisk together eggs, grated cheese, and generous black pepper.",
                        durationMinutes = 2,
                        ingredients = listOf(
                            Ingredient("eggs", "4 large"),
                            Ingredient("Pecorino Romano cheese", "100g", "grated"),
                            Ingredient("black pepper", "to taste")
                        )
                    ),
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
                    RecipeStep(
                        stepNumber = 1,
                        instruction = "Mix chicken with 1 tbsp soy sauce and cornstarch. Let marinate for 10 minutes.",
                        durationMinutes = 10,
                        ingredients = listOf(
                            Ingredient("chicken breast", "500g", "sliced"),
                            Ingredient("soy sauce", "1 tbsp"),
                            Ingredient("cornstarch", "1 tbsp")
                        )
                    ),
                    RecipeStep(
                        stepNumber = 2,
                        instruction = "Heat 1 tbsp oil in a wok or large pan over high heat.",
                        durationMinutes = 2,
                        ingredients = listOf(
                            Ingredient("vegetable oil", "1 tbsp")
                        )
                    ),
                    RecipeStep(3, "Add chicken and stir-fry until golden brown, about 5 minutes. Remove and set aside.", 5),
                    RecipeStep(
                        stepNumber = 4,
                        instruction = "Add remaining oil to the pan. Stir-fry garlic for 30 seconds.",
                        durationMinutes = 1,
                        ingredients = listOf(
                            Ingredient("vegetable oil", "1 tbsp"),
                            Ingredient("garlic", "3 cloves", "minced")
                        )
                    ),
                    RecipeStep(
                        stepNumber = 5,
                        instruction = "Add peppers and onion, stir-fry for 3-4 minutes until slightly softened.",
                        durationMinutes = 4,
                        ingredients = listOf(
                            Ingredient("bell peppers", "2", "sliced"),
                            Ingredient("onion", "1", "sliced")
                        )
                    ),
                    RecipeStep(
                        stepNumber = 6,
                        instruction = "Return chicken to pan, add remaining soy sauce.",
                        durationMinutes = 1,
                        ingredients = listOf(
                            Ingredient("soy sauce", "2 tbsp")
                        )
                    ),
                    RecipeStep(7, "Toss everything together for 1-2 minutes.", 2),
                    RecipeStep(
                        stepNumber = 8,
                        instruction = "Serve hot over cooked rice.",
                        ingredients = listOf(
                            Ingredient("cooked rice", "for serving")
                        )
                    )
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

    fun getRecipeById(id: String): Recipe? {
        return _recipes.value.find { it.id == id }
    }
    fun startCookingMode(recipe: Recipe, initialStepIndex: Int = 0) {
        _activeRecipe.value = recipe
        val validStepIndex = initialStepIndex.coerceIn(0, recipe.steps.size - 1)
        _currentStepIndex.value = validStepIndex

        widgetPreferences?.setActiveCookingSession(recipe.id, validStepIndex)
        triggerWidgetUpdate()
    }

    fun startOrResumeCookingMode(recipe: Recipe, stepIndex: Int = 0) {
        val validStepIndex = stepIndex.coerceIn(0, recipe.steps.size - 1)

        if (_activeRecipe.value?.id == recipe.id) {
            Log.d(TAG, "Resuming cooking mode for ${recipe.name}, navigating to step $validStepIndex")
            _currentStepIndex.value = validStepIndex
            widgetPreferences?.setActiveCookingSession(recipe.id, validStepIndex)
            triggerWidgetUpdate()
        } else {
            Log.d(TAG, "Starting new cooking mode for ${recipe.name} at step $validStepIndex")
            startCookingMode(recipe, validStepIndex)
        }
    }

    fun hasActiveTimersForRecipe(recipeId: String): Boolean {
        if (_timers.value.isNotEmpty()) {
            return _timers.value.values.any {
                it.recipeId == recipeId &&
                (it.status == TimerStatus.RUNNING || it.status == TimerStatus.PAUSED)
            }
        }
        return timerServiceBridge?.hasActiveTimersForRecipe(recipeId) ?: false
    }

    fun restoreTimerStateFromService(recipeId: String) {
        val activeTimers = timerServiceBridge?.getActiveTimersForRecipe(recipeId) ?: return

        if (activeTimers.isEmpty()) {
            Log.d(TAG, "No active timers to restore for recipe $recipeId")
            return
        }

        Log.d(TAG, "Restoring ${activeTimers.size} timer(s) from service for recipe $recipeId")

        activeTimers.forEach { timerData ->
            val timerState = TimerState(
                stepIndex = timerData.stepIndex,
                recipeId = recipeId,
                durationSeconds = timerData.durationSeconds,
                remainingSeconds = timerData.remainingSeconds,
                status = TimerStatus.RUNNING,
                startTimestamp = System.currentTimeMillis(),
                timerId = timerData.timerId
            )
            _timers.value = _timers.value + (timerData.stepIndex to timerState)
            Log.d(TAG, "Restored timer for step ${timerData.stepIndex}: ${timerData.remainingSeconds}s remaining (ID: ${timerData.timerId})")
        }
    }
    fun startCookingFresh(recipe: Recipe, stepIndex: Int = 0) {
        Log.d(TAG, "Starting cooking fresh for ${recipe.name}")

        val existingTimerSteps = _timers.value
            .filter { it.value.recipeId == recipe.id }
            .keys.toList()

        if (existingTimerSteps.isNotEmpty()) {
            timerServiceBridge?.stopAllTimersAndCleanup(existingTimerSteps)
        }

        _timers.value = _timers.value.filterNot { it.value.recipeId == recipe.id }

        startCookingMode(recipe, stepIndex)
    }

    fun nextStep(): Boolean {
        val recipe = _activeRecipe.value ?: return false
        val currentIndex = _currentStepIndex.value
        val maxIndex = recipe.steps.size - 1

        return if (currentIndex < maxIndex) {
            _currentStepIndex.value = currentIndex + 1

            widgetPreferences?.setActiveCookingSession(recipe.id, currentIndex + 1)
            triggerWidgetUpdate()

            true
        } else {
            false
        }
    }
    fun previousStep(): Boolean {
        val recipe = _activeRecipe.value
        val currentIndex = _currentStepIndex.value

        return if (currentIndex > 0) {
            _currentStepIndex.value = currentIndex - 1

            recipe?.let {
                widgetPreferences?.setActiveCookingSession(it.id, currentIndex - 1)
                triggerWidgetUpdate()
            }

            true
        } else {
            false
        }
    }

    fun goToStep(stepIndex: Int) {
        val recipe = _activeRecipe.value ?: return
        val maxIndex = recipe.steps.size - 1

        if (stepIndex in 0..maxIndex) {
            _currentStepIndex.value = stepIndex

            widgetPreferences?.setActiveCookingSession(recipe.id, stepIndex)
            triggerWidgetUpdate()
        }
    }

    fun resetToFirstStep() {
        val recipe = _activeRecipe.value
        _currentStepIndex.value = 0

        recipe?.let {
            widgetPreferences?.setActiveCookingSession(it.id, 0)
            triggerWidgetUpdate()
        }
    }

    fun startTimer() {
        val recipe = _activeRecipe.value ?: run {
            Log.w(TAG, "Cannot start timer: no active recipe")
            return
        }
        val stepIndex = _currentStepIndex.value
        val step = recipe.steps.getOrNull(stepIndex) ?: run {
            Log.w(TAG, "Cannot start timer: invalid step index $stepIndex")
            return
        }
        val durationMinutes = step.durationMinutes ?: run {
            Log.w(TAG, "Cannot start timer: step has no duration")
            return
        }

        val timerState = TimerState(
            stepIndex = stepIndex,
            recipeId = recipe.id,
            durationSeconds = durationMinutes * 60,
            remainingSeconds = durationMinutes * 60,
            status = TimerStatus.RUNNING,
            startTimestamp = System.currentTimeMillis()
        )

        _timers.value = _timers.value + (stepIndex to timerState)
        timerServiceBridge?.startTimer(timerState)

        widgetPreferences?.setActiveTimerRecipe(recipe.id)
        triggerWidgetUpdate()

        Log.d(TAG, "Started timer for step $stepIndex: ${durationMinutes}min (ID: ${timerState.timerId})")
    }

    fun pauseTimer() {
        val stepIndex = _currentStepIndex.value
        val timer = _timers.value[stepIndex] ?: run {
            Log.w(TAG, "Cannot pause timer: no timer for step $stepIndex")
            return
        }

        if (timer.status != TimerStatus.RUNNING) {
            Log.w(TAG, "Cannot pause timer: timer is not running (status: ${timer.status})")
            return
        }

        val updatedTimer = timer.copy(
            status = TimerStatus.PAUSED,
            pauseTimestamp = System.currentTimeMillis()
        )

        _timers.value = _timers.value + (stepIndex to updatedTimer)
        timerServiceBridge?.pauseTimer(timer.timerId)

        Log.d(TAG, "Paused timer for step $stepIndex")
    }

    fun resumeTimer() {
        val stepIndex = _currentStepIndex.value
        val timer = _timers.value[stepIndex] ?: run {
            Log.w(TAG, "Cannot resume timer: no timer for step $stepIndex")
            return
        }

        if (timer.status != TimerStatus.PAUSED) {
            Log.w(TAG, "Cannot resume timer: timer is not paused (status: ${timer.status})")
            return
        }

        val updatedTimer = timer.copy(
            status = TimerStatus.RUNNING,
            startTimestamp = System.currentTimeMillis(),
            pauseTimestamp = null
        )

        _timers.value = _timers.value + (stepIndex to updatedTimer)
        timerServiceBridge?.resumeTimer(timer.timerId, updatedTimer)

        Log.d(TAG, "Resumed timer for step $stepIndex")
    }

    fun stopTimer() {
        val stepIndex = _currentStepIndex.value
        val timer = _timers.value[stepIndex] ?: run {
            Log.w(TAG, "Cannot stop timer: no timer for step $stepIndex")
            return
        }

        val updatedTimer = timer.copy(status = TimerStatus.CANCELLED)
        _timers.value = _timers.value + (stepIndex to updatedTimer)

        timerServiceBridge?.stopTimer(timer.timerId)

        Log.d(TAG, "Stopped timer for step $stepIndex")

        viewModelScope.launch {
            delay(2000)
            _timers.value = _timers.value - stepIndex
        }
    }

    fun updateTimerState(timerId: String, remainingSeconds: Int) {
        val stepIndex = _timers.value.entries.find { it.value.timerId == timerId }?.key ?: run {
            Log.w(TAG, "Cannot update timer: timer ID $timerId not found")
            return
        }
        val timer = _timers.value[stepIndex] ?: return

        val updatedTimer = timer.copy(remainingSeconds = remainingSeconds)
        _timers.value = _timers.value + (stepIndex to updatedTimer)

        if (remainingSeconds <= 0 && timer.status == TimerStatus.RUNNING) {
            onTimerFinished(timerId)
        }
    }

    private fun onTimerFinished(timerId: String) {
        val stepIndex = _timers.value.entries.find { it.value.timerId == timerId }?.key ?: return
        val timer = _timers.value[stepIndex] ?: return

        val updatedTimer = timer.copy(status = TimerStatus.FINISHED)
        _timers.value = _timers.value + (stepIndex to updatedTimer)

        timerAlarmCallback?.onTimerFinished(timer)

        Log.d(TAG, "Timer finished for step $stepIndex")
    }

    fun getAllActiveTimers(): List<Pair<Int, TimerState>> {
        return _timers.value
            .filter { it.value.status == TimerStatus.RUNNING || it.value.status == TimerStatus.PAUSED }
            .toList()
            .sortedBy { it.first }
    }
    fun setTimerServiceBridge(bridge: TimerServiceBridge?) {
        timerServiceBridge = bridge
    }

    fun setTimerAlarmCallback(callback: TimerAlarmCallback?) {
        timerAlarmCallback = callback
    }

    fun setTtsCallback(callback: TtsCallback?) {
        ttsCallback = callback
    }
    fun toggleTts() {
        _isTtsEnabled.value = !_isTtsEnabled.value
    }

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
    }
    private fun autoSpeakCurrentStep() {
        if (!_isTtsEnabled.value) return

        val recipe = _activeRecipe.value ?: return
        val stepIndex = _currentStepIndex.value
        if (stepIndex >= recipe.steps.size) return

        val step = recipe.steps[stepIndex]
        ttsCallback?.speakInstruction(step.instruction)
    }

    fun processVoiceCommand(command: VoiceCommand) {
        val recipe = _activeRecipe.value
        val stepIndex = _currentStepIndex.value
        val step = recipe?.steps?.getOrNull(stepIndex)

        when (command) {
            VoiceCommand.NEXT -> {
                if (nextStep()) {
                    autoSpeakCurrentStep()
                }
            }
            VoiceCommand.PREVIOUS -> {
                if (previousStep()) {
                    autoSpeakCurrentStep()
                }
            }
            VoiceCommand.START -> {
                resetToFirstStep()
                autoSpeakCurrentStep()
            }
            VoiceCommand.REPEAT -> {
                step?.let { ttsCallback?.speakInstruction(it.instruction) }
            }

            VoiceCommand.INGREDIENTS -> {
                Log.d(TAG, "INGREDIENTS command - step: $step, callback: ${ttsCallback != null}")
                step?.let {
                    if (it.ingredients.isNotEmpty()) {
                        Log.d(TAG, "Speaking ${it.ingredients.size} step ingredients")
                        ttsCallback?.speakIngredients(it.ingredients)
                    } else {
                        Log.d(TAG, "No step ingredients, using recipe-level (${recipe?.ingredients?.size} items)")
                        recipe?.let { r -> ttsCallback?.speakIngredients(r.ingredients) }
                    }
                }
            }
            VoiceCommand.DESCRIPTION -> {
                Log.d(TAG, "DESCRIPTION command - step: $step, callback: ${ttsCallback != null}")
                step?.let { ttsCallback?.speakInstruction(it.instruction) }
            }
            VoiceCommand.TIME -> {
                Log.d(TAG, "TIME command - duration: ${step?.durationMinutes}, callback: ${ttsCallback != null}")
                step?.durationMinutes?.let { duration ->
                    ttsCallback?.speakDuration(duration)
                }
            }
            VoiceCommand.TIPS -> {
                step?.tips?.let { tips ->
                    ttsCallback?.speakTips(tips)
                }
            }
            VoiceCommand.STEP_NUMBER -> {
                recipe?.let {
                    ttsCallback?.speakStepNumber(stepIndex + 1, it.steps.size)
                }
            }

            VoiceCommand.START_TIMER -> {
                Log.d(TAG, "START_TIMER command - step: $stepIndex, duration: ${step?.durationMinutes}")
                if (step?.durationMinutes != null) {
                    val timer = _timers.value[stepIndex]
                    if (timer == null || timer.status == TimerStatus.CANCELLED) {
                        startTimer()
                        ttsCallback?.speakTimerStarted(step.durationMinutes)
                    } else {
                        ttsCallback?.speakMessage("Timer already running")
                        Log.d(TAG, "Timer already exists for step $stepIndex (status: ${timer.status})")
                    }
                } else {
                    ttsCallback?.speakMessage("This step has no duration. Cannot start timer.")
                    Log.d(TAG, "Cannot start timer: step has no duration")
                }
            }

            VoiceCommand.PAUSE_TIMER -> {
                Log.d(TAG, "PAUSE_TIMER command - step: $stepIndex")
                val timer = _timers.value[stepIndex]
                if (timer?.status == TimerStatus.RUNNING) {
                    pauseTimer()
                    val minutes = timer.remainingSeconds / 60
                    val seconds = timer.remainingSeconds % 60
                    ttsCallback?.speakTimerPaused(minutes, seconds)
                } else {
                    ttsCallback?.speakMessage("No timer running for this step")
                    Log.d(TAG, "Cannot pause timer: no running timer for step $stepIndex")
                }
            }

            VoiceCommand.RESUME_TIMER -> {
                Log.d(TAG, "RESUME_TIMER command - step: $stepIndex")
                val timer = _timers.value[stepIndex]
                if (timer?.status == TimerStatus.PAUSED) {
                    resumeTimer()
                    ttsCallback?.speakTimerResumed()
                } else {
                    ttsCallback?.speakMessage("No paused timer for this step")
                    Log.d(TAG, "Cannot resume timer: no paused timer for step $stepIndex")
                }
            }

            VoiceCommand.STOP_TIMER -> {
                Log.d(TAG, "STOP_TIMER command - step: $stepIndex")
                val timer = _timers.value[stepIndex]
                if (timer != null && (timer.status == TimerStatus.RUNNING || timer.status == TimerStatus.PAUSED)) {
                    stopTimer()
                    ttsCallback?.speakTimerCancelled()
                } else {
                    ttsCallback?.speakMessage("No active timer for this step")
                    Log.d(TAG, "Cannot stop timer: no active timer for step $stepIndex")
                }
            }

            VoiceCommand.CHECK_TIMER -> {
                Log.d(TAG, "CHECK_TIMER command - step: $stepIndex")
                val timer = _timers.value[stepIndex]
                if (timer?.status == TimerStatus.RUNNING || timer?.status == TimerStatus.PAUSED) {
                    val minutes = timer.remainingSeconds / 60
                    val seconds = timer.remainingSeconds % 60
                    ttsCallback?.speakTimerStatus(minutes, seconds)
                } else {
                    ttsCallback?.speakMessage("No timer running for this step")
                    Log.d(TAG, "No timer to check for step $stepIndex")
                }
            }
        }
    }

    fun exitCookingMode() {
        val stepIndices = _timers.value.keys.toList()

        if (stepIndices.isNotEmpty()) {
            timerServiceBridge?.stopAllTimersAndCleanup(stepIndices)
        }
        _timers.value = emptyMap()

        _activeRecipe.value = null
        _currentStepIndex.value = 0

        widgetPreferences?.clearCookingSession()
        widgetPreferences?.clearActiveTimerRecipe()
        triggerWidgetUpdate()

        ttsCallback = null
        timerServiceBridge = null
        timerAlarmCallback = null

        Log.d(TAG, "Exited cooking mode, cleaned up all timers")
    }

    private fun triggerWidgetUpdate() {
        context?.let { ctx ->
            WidgetUpdater.updateWidgets(ctx)
            Log.d(TAG, "Triggered widget update from ViewModel")
        }
    }

    fun saveRecipe(recipe: Recipe) {
        if (repository != null) {
            viewModelScope.launch {
                repository.saveRecipe(recipe)
                    .onSuccess { recipeId ->
                        Log.d(TAG, "Recipe saved with ID: $recipeId")
                        loadRecipes()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to save recipe", error)
                    }
            }
        } else {
            Log.w(TAG, "Cannot save recipe: repository is null")
        }
    }
    fun updateRecipe(recipe: Recipe) {
        if (repository != null) {
            viewModelScope.launch {
                repository.updateRecipe(recipe)
                    .onSuccess {
                        Log.d(TAG, "Recipe updated: ${recipe.id}")
                        loadRecipes()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to update recipe", error)
                    }
            }
        } else {
            Log.w(TAG, "Cannot update recipe: repository is null")
        }
    }

    fun deleteRecipe(recipeId: String) {
        if (repository != null) {
            viewModelScope.launch {
                repository.deleteRecipe(recipeId)
                    .onSuccess {
                        Log.d(TAG, "Recipe deleted: $recipeId")
                        loadRecipes()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to delete recipe", error)
                    }
            }
        } else {
            Log.w(TAG, "Cannot delete recipe: repository is null")
        }
    }

    fun saveDraft(draft: RecipeDraft) {
        Log.d(TAG, "Saving recipe draft")
        _recipeDraft.value = draft
    }

    fun loadDraft(): RecipeDraft? {
        return _recipeDraft.value
    }

    fun clearDraft() {
        Log.d(TAG, "Clearing recipe draft")
        _recipeDraft.value = null
    }

    fun hasDraft(): Boolean {
        return _recipeDraft.value != null
    }
}
