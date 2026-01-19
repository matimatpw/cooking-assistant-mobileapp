package com.example.cookingassistant.model

import kotlinx.serialization.Serializable

/**
 * Represents a user's swipe action on a recipe
 *
 * @param recipeId The ID of the recipe that was swiped
 * @param action The swipe action taken (LIKE, DISLIKE, SUPER_LIKE, SKIP)
 * @param timestamp When the swipe occurred
 */
@Serializable
data class SwipeAction(
    val recipeId: String,
    val action: SwipeType,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of swipe actions available
 */
@Serializable
enum class SwipeType {
    LIKE,           // Swipe right - add to favorites/cooking queue
    DISLIKE,        // Swipe left - don't show again
    SUPER_LIKE,     // Swipe up - add to priority cooking queue
    SKIP            // Swipe down - skip for now, may show again later
}

/**
 * Represents the current state of the swipe deck
 *
 * @param currentRecipe The recipe currently being displayed
 * @param nextRecipes Preview of upcoming recipes (for pre-loading)
 * @param hasMore Whether there are more recipes available
 * @param isLoading Whether new recipes are being loaded
 */
data class SwipeDeckState(
    val currentRecipe: Recipe? = null,
    val nextRecipes: List<Recipe> = emptyList(),
    val hasMore: Boolean = true,
    val isLoading: Boolean = false
)

/**
 * User preferences for recipe discovery via swiping
 *
 * @param preferredCategories Categories the user prefers to see
 * @param excludedCategories Categories to exclude from recommendations
 * @param maxCookingTime Maximum cooking time in minutes (null = no limit)
 * @param minCookingTime Minimum cooking time in minutes (null = no limit)
 * @param preferredDifficulties Difficulty levels user prefers
 * @param showPreviouslySkipped Whether to show recipes that were previously skipped
 * @param excludeDisliked Whether to exclude previously disliked recipes
 */
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
    /**
     * Check if any preferences are set
     */
    fun hasActivePreferences(): Boolean {
        return preferredCategories.isNotEmpty() ||
                excludedCategories.isNotEmpty() ||
                maxCookingTime != null ||
                minCookingTime != null ||
                preferredDifficulties.isNotEmpty()
    }
}

/**
 * Tracks user's swipe history and statistics
 *
 * @param totalSwipes Total number of swipes performed
 * @param likeCount Number of liked recipes
 * @param dislikeCount Number of disliked recipes
 * @param superLikeCount Number of super-liked recipes
 * @param skipCount Number of skipped recipes
 * @param recentActions Recent swipe actions (limited to last 100)
 * @param likedRecipeIds Set of liked recipe IDs for quick lookup
 * @param dislikedRecipeIds Set of disliked recipe IDs for filtering
 * @param skippedRecipeIds Set of skipped recipe IDs
 */
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
    /**
     * Add a new swipe action to the history
     */
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

    /**
     * Check if a recipe has been liked
     */
    fun isLiked(recipeId: String): Boolean = recipeId in likedRecipeIds

    /**
     * Check if a recipe has been disliked
     */
    fun isDisliked(recipeId: String): Boolean = recipeId in dislikedRecipeIds

    /**
     * Check if a recipe has been skipped
     */
    fun isSkipped(recipeId: String): Boolean = recipeId in skippedRecipeIds
}

/**
 * Represents a swipe gesture state for animation
 *
 * @param offsetX Horizontal offset in pixels
 * @param offsetY Vertical offset in pixels
 * @param rotation Rotation angle in degrees
 */
data class SwipeGestureState(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f
) {
    /**
     * Determine the swipe type based on offsets
     * Returns null if the swipe is not strong enough
     */
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

/**
 * Complete swipe session data combining all swipe-related information
 *
 * @param deckState Current state of the swipe deck
 * @param history User's swipe history
 * @param preferences User's swipe preferences
 */
data class SwipeSession(
    val deckState: SwipeDeckState = SwipeDeckState(),
    val history: SwipeHistory = SwipeHistory(),
    val preferences: SwipePreferences = SwipePreferences()
)