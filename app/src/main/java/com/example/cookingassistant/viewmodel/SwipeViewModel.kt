package com.example.cookingassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookingassistant.model.*
import com.example.cookingassistant.repository.CachedRecipeRepository
import com.example.cookingassistant.repository.SwipeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SwipeViewModel(
    private val recipeRepository: CachedRecipeRepository,
    private val swipeRepository: SwipeRepository
) : ViewModel() {

    private val _swipeSession = MutableStateFlow(SwipeSession())
    val swipeSession: StateFlow<SwipeSession> = _swipeSession.asStateFlow()

    private val _gestureState = MutableStateFlow(SwipeGestureState())
    val gestureState: StateFlow<SwipeGestureState> = _gestureState.asStateFlow()

    private var availableRecipes: List<Recipe> = emptyList()
    private var currentIndex = 0

    init {
        viewModelScope.launch {
            combine(
                swipeRepository.swipeHistory,
                swipeRepository.swipePreferences
            ) { history, preferences ->
                _swipeSession.update { it.copy(history = history, preferences = preferences) }
            }.collect()
        }

        loadRecipes()
    }

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
    private fun filterRecipes(recipes: List<Recipe>): List<Recipe> {
        val preferences = _swipeSession.value.preferences
        val history = _swipeSession.value.history

        return recipes.filter { recipe ->
            if (preferences.excludeDisliked && history.isDisliked(recipe.id)) {
                return@filter false
            }

            if (!preferences.showPreviouslySkipped && history.isSkipped(recipe.id)) {
                return@filter false
            }

            if (preferences.preferredCategories.isNotEmpty() &&
                recipe.categories.none { it in preferences.preferredCategories }) {
                return@filter false
            }

            if (recipe.categories.any { it in preferences.excludedCategories }) {
                return@filter false
            }

            if (preferences.maxCookingTime != null && recipe.cookingTime > preferences.maxCookingTime) {
                return@filter false
            }
            if (preferences.minCookingTime != null && recipe.cookingTime < preferences.minCookingTime) {
                return@filter false
            }

            if (preferences.preferredDifficulties.isNotEmpty() &&
                recipe.difficulty !in preferences.preferredDifficulties) {
                return@filter false
            }

            true
        }.shuffled()
    }

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

    fun updateGesture(offsetX: Float, offsetY: Float, rotation: Float) {
        _gestureState.value = SwipeGestureState(offsetX, offsetY, rotation)
    }

    fun resetGesture() {
        _gestureState.value = SwipeGestureState()
    }

    fun swipe(swipeType: SwipeType) {
        val currentRecipe = _swipeSession.value.deckState.currentRecipe ?: return

        val action = SwipeAction(
            recipeId = currentRecipe.id,
            action = swipeType
        )
        swipeRepository.recordSwipe(action)

        currentIndex++
        updateDeckState()
        resetGesture()

        if (availableRecipes.size - currentIndex < 5) {
            loadRecipes()
        }
    }

    fun undoSwipe() {
        if (currentIndex > 0) {
            currentIndex--
            updateDeckState()
        }
    }

    fun updatePreferences(preferences: SwipePreferences) {
        swipeRepository.updatePreferences(preferences)
        loadRecipes()
    }

    suspend fun getLikedRecipes(): Result<List<Recipe>> {
        val likedIds = _swipeSession.value.history.likedRecipeIds

        return recipeRepository.getAllRecipes().map { recipes ->
            recipes.filter { it.id in likedIds }
        }
    }

}