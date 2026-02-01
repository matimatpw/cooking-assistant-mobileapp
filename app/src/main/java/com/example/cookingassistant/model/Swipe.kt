package com.example.cookingassistant.model

import kotlinx.serialization.Serializable

@Serializable
data class SwipeAction(
    val recipeId: String,
    val action: SwipeType,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class SwipeType {
    LIKE,           // Swipe right - add to favorites/cooking queue
    DISLIKE,        // Swipe left - don't show again
    SUPER_LIKE,     // Swipe up - add to priority cooking queue
    SKIP            // Swipe down - skip for now, may show again later
}

data class SwipeDeckState(
    val currentRecipe: Recipe? = null,
    val nextRecipes: List<Recipe> = emptyList(),
    val hasMore: Boolean = true,
    val isLoading: Boolean = false
)

@Serializable
data class SwipePreferences(
    val preferredCategories: Set<RecipeCategory> = emptySet(),
    val excludedCategories: Set<RecipeCategory> = emptySet(),
    val maxCookingTime: Int? = null,
    val minCookingTime: Int? = null,
    val preferredDifficulties: Set<Difficulty> = emptySet(),
    val showPreviouslySkipped: Boolean = true,
    val excludeDisliked: Boolean = true
) {
    fun hasActivePreferences(): Boolean {
        return preferredCategories.isNotEmpty() ||
                excludedCategories.isNotEmpty() ||
                maxCookingTime != null ||
                minCookingTime != null ||
                preferredDifficulties.isNotEmpty()
    }
}

@Serializable
data class SwipeHistory(
    val totalSwipes: Int = 0,
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val superLikeCount: Int = 0,
    val skipCount: Int = 0,
    val recentActions: List<SwipeAction> = emptyList(),
    val likedRecipeIds: Set<String> = emptySet(),
    val dislikedRecipeIds: Set<String> = emptySet(),
    val skippedRecipeIds: Set<String> = emptySet()
) {
    fun addAction(action: SwipeAction): SwipeHistory {
        val updatedRecent = (listOf(action) + recentActions).take(100)

        return when (action.action) {
            SwipeType.LIKE -> copy(
                totalSwipes = totalSwipes + 1,
                likeCount = likeCount + 1,
                recentActions = updatedRecent,
                likedRecipeIds = likedRecipeIds + action.recipeId,
                skippedRecipeIds = skippedRecipeIds - action.recipeId // Remove from skipped if liked
            )
            SwipeType.DISLIKE -> copy(
                totalSwipes = totalSwipes + 1,
                dislikeCount = dislikeCount + 1,
                recentActions = updatedRecent,
                dislikedRecipeIds = dislikedRecipeIds + action.recipeId,
                skippedRecipeIds = skippedRecipeIds - action.recipeId // Remove from skipped if disliked
            )
            SwipeType.SUPER_LIKE -> copy(
                totalSwipes = totalSwipes + 1,
                superLikeCount = superLikeCount + 1,
                recentActions = updatedRecent,
                likedRecipeIds = likedRecipeIds + action.recipeId,
                skippedRecipeIds = skippedRecipeIds - action.recipeId // Remove from skipped if super-liked
            )
            SwipeType.SKIP -> copy(
                totalSwipes = totalSwipes + 1,
                skipCount = skipCount + 1,
                recentActions = updatedRecent,
                skippedRecipeIds = skippedRecipeIds + action.recipeId
            )
        }
    }

    fun isLiked(recipeId: String): Boolean = recipeId in likedRecipeIds

    fun isDisliked(recipeId: String): Boolean = recipeId in dislikedRecipeIds

    fun isSkipped(recipeId: String): Boolean = recipeId in skippedRecipeIds
}

data class SwipeGestureState(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f
) {
    fun determineSwipeType(threshold: Float = 300f): SwipeType? {
        return when {
            offsetX > threshold -> SwipeType.LIKE
            offsetX < -threshold -> SwipeType.DISLIKE
            offsetY < -threshold -> SwipeType.SUPER_LIKE
            offsetY > threshold -> SwipeType.SKIP
            else -> null
        }
    }
}

data class SwipeSession(
    val deckState: SwipeDeckState = SwipeDeckState(),
    val history: SwipeHistory = SwipeHistory(),
    val preferences: SwipePreferences = SwipePreferences()
)