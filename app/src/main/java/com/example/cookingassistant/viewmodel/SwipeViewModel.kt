package com.example.cookingassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingassistant.model.*
import com.example.cookingassistant.repository.CachedRecipeRepository
import com.example.cookingassistant.repository.SwipeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing recipe swiping functionality
 */
class SwipeViewModel(
    private val recipeRepository: CachedRecipeRepository,
    private val swipeRepository: SwipeRepository
) : ViewModel() {

    // Swipe session state
    private val _swipeSession = MutableStateFlow(SwipeSession())
    val swipeSession: StateFlow<SwipeSession> = _swipeSession.asStateFlow()

    // Current gesture state for animations
    private val _gestureState = MutableStateFlow(SwipeGestureState())
    val gestureState: StateFlow<SwipeGestureState> = _gestureState.asStateFlow()

    // Available recipes for swiping
    private var availableRecipes: List<Recipe> = emptyList()
    private var currentIndex = 0

    init {
        // Load initial data
        viewModelScope.launch {
            // Combine swipe history and preferences
            combine(
                swipeRepository.swipeHistory,
                swipeRepository.swipePreferences
            ) { history, preferences ->
                _swipeSession.update { it.copy(history = history, preferences = preferences) }
            }.collect()
        }

        // Load initial recipes
        loadRecipes()
    }

    /**
     * Load recipes based on current preferences
     */
    fun loadRecipes() {
        viewModelScope.launch {
            _swipeSession.update { it.copy(deckState = it.deckState.copy(isLoading = true)) }

            try {
                val result =  recipeRepository.getAllRecipes();
                result.onSuccess { recipes ->
                    availableRecipes = filterRecipes(recipes)
                    currentIndex = 0
                    updateDeckState()
                }.onFailure {
                    println("Error loading recipes")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _swipeSession.update { it.copy(deckState = it.deckState.copy(isLoading = false)) }
            }
        }
    }

    /**
     * Filter recipes based on preferences and history
     */
    private fun filterRecipes(recipes: List<Recipe>): List<Recipe> {
        val preferences = _swipeSession.value.preferences
        val history = _swipeSession.value.history

        return recipes.filter { recipe ->
            // Filter out disliked recipes if preference is set
            if (preferences.excludeDisliked && history.isDisliked(recipe.id)) {
                return@filter false
            }

            // Filter out skipped recipes if preference is set
            if (!preferences.showPreviouslySkipped && history.isSkipped(recipe.id)) {
                return@filter false
            }

            // Filter by preferred categories
            if (preferences.preferredCategories.isNotEmpty() &&
                recipe.categories.none { it in preferences.preferredCategories }) {
                return@filter false
            }

            // Filter out excluded categories
            if (recipe.categories.any { it in preferences.excludedCategories }) {
                return@filter false
            }

            // Filter by cooking time
            if (preferences.maxCookingTime != null && recipe.cookingTime > preferences.maxCookingTime) {
                return@filter false
            }
            if (preferences.minCookingTime != null && recipe.cookingTime < preferences.minCookingTime) {
                return@filter false
            }

            // Filter by difficulty
            if (preferences.preferredDifficulties.isNotEmpty() &&
                recipe.difficulty !in preferences.preferredDifficulties) {
                return@filter false
            }

            true
        }.shuffled() // Randomize order for discovery
    }

    /**
     * Update deck state with current and next recipes
     */
    private fun updateDeckState() {
        val current = availableRecipes.getOrNull(currentIndex)
        val next = availableRecipes.drop(currentIndex + 1).take(3)
        val hasMore = currentIndex < availableRecipes.size

        _swipeSession.update {
            it.copy(
                deckState = SwipeDeckState(
                    currentRecipe = current,
                    nextRecipes = next,
                    hasMore = hasMore,
                    isLoading = false
                )
            )
        }
    }

    /**
     * Update gesture state for animations
     */
    fun updateGesture(offsetX: Float, offsetY: Float, rotation: Float) {
        _gestureState.value = SwipeGestureState(offsetX, offsetY, rotation)
    }

    /**
     * Reset gesture state
     */
    fun resetGesture() {
        _gestureState.value = SwipeGestureState()
    }

    /**
     * Perform a swipe action
     */
    fun swipe(swipeType: SwipeType) {
        val currentRecipe = _swipeSession.value.deckState.currentRecipe ?: return

        // Record the swipe action
        val action = SwipeAction(
            recipeId = currentRecipe.id,
            action = swipeType
        )
        swipeRepository.recordSwipe(action)

        // Move to next recipe
        currentIndex++
        updateDeckState()
        resetGesture()

        // Reload if running low on recipes
        if (availableRecipes.size - currentIndex < 5) {
            loadRecipes()
        }
    }

    /**
     * Undo the last swipe
     */
    fun undoSwipe() {
        if (currentIndex > 0) {
            currentIndex--
            updateDeckState()
        }
    }

    /**
     * Update swipe preferences
     */
    fun updatePreferences(preferences: SwipePreferences) {
        swipeRepository.updatePreferences(preferences)
        loadRecipes() // Reload recipes with new preferences
    }

    /**
     * Get liked recipes
     */
    suspend fun getLikedRecipes(): Result<List<Recipe>> {
        val likedIds = _swipeSession.value.history.likedRecipeIds

        return recipeRepository.getAllRecipes().map { recipes ->
            recipes.filter { it.id in likedIds }
        }
    }

}