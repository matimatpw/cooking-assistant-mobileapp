package com.example.cookingassistant.model

import kotlinx.serialization.Serializable

/**
 * Enhanced data model representing a recipe with multimedia support
 *
 * @param id Unique identifier (UUID string for custom recipes, simple ID for bundled)
 * @param name Name of the dish
 * @param description Short description of the dish
 * @param mainPhotoUri URI to the main dish photo (relative path)
 * @param ingredients List of ingredients with quantities
 * @param steps Step-by-step cooking instructions with multimedia support
 * @param cookingTime Estimated cooking time in minutes
 * @param prepTime Estimated preparation time in minutes
 * @param servings Number of servings this recipe makes
 * @param difficulty Cooking difficulty level
 * @param categories Set of categories this recipe belongs to
 * @param tags User-defined or flexible tags
 * @param createdAt Creation timestamp
 * @param updatedAt Last modification timestamp
 * @param isCustom Whether this is a user-created recipe
 */
@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val description: String,
    val mainPhotoUri: String? = null,
    val ingredients: List<Ingredient>,
    val steps: List<RecipeStep>,
    val cookingTime: Int,
    val prepTime: Int,
    val servings: Int,
    val difficulty: Difficulty,
    val categories: Set<RecipeCategory>,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val isCustom: Boolean = false
)

/**
 * Represents an ingredient with quantity information
 *
 * @param name Name of the ingredient
 * @param quantity Quantity string (e.g., "400g", "2 tbsp")
 * @param notes Optional notes (e.g., "sliced", "optional")
 */
@Serializable
data class Ingredient(
    val name: String,
    val quantity: String,
    val notes: String? = null
)

/**
 * Represents a cooking step with multimedia support
 *
 * @param stepNumber Sequential step number (1-indexed)
 * @param instruction Text instruction for this step
 * @param durationMinutes Optional duration for this specific step
 * @param mediaItems List of photos/videos for this step
 * @param tips Optional cooking tips for this step
 * @param ingredients List of ingredients needed for this specific step (falls back to recipe-level if empty)
 */
@Serializable
data class RecipeStep(
    val stepNumber: Int,
    val instruction: String,
    val durationMinutes: Int? = null,
    val mediaItems: List<StepMedia> = emptyList(),
    val tips: String? = null,
    val ingredients: List<Ingredient> = emptyList()
)

/**
 * Represents a media item (photo or video) for a recipe step
 *
 * @param type Type of media (PHOTO or VIDEO)
 * @param uri Relative path to the media file
 * @param caption Optional caption for the media
 * @param thumbnailUri Optional thumbnail URI (mainly for videos)
 */
@Serializable
data class StepMedia(
    val type: MediaType,
    val uri: String,
    val caption: String? = null,
    val thumbnailUri: String? = null
)

/**
 * Type of media attachment
 */
@Serializable
enum class MediaType {
    PHOTO,
    VIDEO
}

/**
 * Difficulty level of a recipe
 */
@Serializable
enum class Difficulty {
    EASY,
    MEDIUM,
    HARD
}

/**
 * Recipe categories for filtering and organization
 */
@Serializable
enum class RecipeCategory {
    // Meal types
    BREAKFAST,
    LUNCH,
    DINNER,
    DESSERT,
    SNACK,
    APPETIZER,

    // Dietary restrictions
    VEGETARIAN,
    VEGAN,
    GLUTEN_FREE,
    DAIRY_FREE,

    // Cuisine types
    ITALIAN,
    POLISH,
    ASIAN,
    MEXICAN,
    GREEK,
    AMERICAN,

    // Occasion/style
    QUICK_MEAL,
    MEAL_PREP,
    PARTY,
    HOLIDAY
}

/**
 * Represents a draft of a recipe being edited
 * Used to save user progress when exiting the add/edit screen without saving
 *
 * @param recipeId ID of recipe being edited, null if creating new recipe
 * @param name Recipe name
 * @param description Recipe description
 * @param mainPhotoUri Main photo URI
 * @param prepTime Preparation time string
 * @param cookingTime Cooking time string
 * @param servings Servings string
 * @param difficulty Difficulty level
 * @param selectedCategories Selected categories
 * @param tags Tags as comma-separated string
 * @param ingredients List of ingredients
 * @param steps List of recipe steps
 * @param timestamp When the draft was saved
 */
data class RecipeDraft(
    val recipeId: String? = null,
    val name: String = "",
    val description: String = "",
    val mainPhotoUri: String? = null,
    val prepTime: String = "",
    val cookingTime: String = "",
    val servings: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val selectedCategories: Set<RecipeCategory> = emptySet(),
    val tags: String = "",
    val ingredients: List<Ingredient> = listOf(Ingredient("", "", null)),
    val steps: List<RecipeStep> = listOf(RecipeStep(1, "", null, emptyList(), null)),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents filtering criteria for recipes
 *
 * @param mealTypes Filter by meal types (breakfast, lunch, dinner, etc.)
 * @param dietaryPreferences Filter by dietary restrictions (vegetarian, vegan, etc.)
 * @param cuisines Filter by cuisine types (Italian, Asian, etc.)
 * @param difficulties Filter by difficulty levels (easy, medium, hard)
 * @param maxCookingTime Maximum cooking time in minutes (null = no limit)
 * @param minCookingTime Minimum cooking time in minutes (null = no limit)
 */
data class RecipeFilters(
    val mealTypes: Set<RecipeCategory> = emptySet(),
    val dietaryPreferences: Set<RecipeCategory> = emptySet(),
    val cuisines: Set<RecipeCategory> = emptySet(),
    val difficulties: Set<Difficulty> = emptySet(),
    val maxCookingTime: Int? = null,
    val minCookingTime: Int? = null
) {
    /**
     * Check if any filters are active
     */
    fun hasActiveFilters(): Boolean {
        return mealTypes.isNotEmpty() ||
                dietaryPreferences.isNotEmpty() ||
                cuisines.isNotEmpty() ||
                difficulties.isNotEmpty() ||
                maxCookingTime != null ||
                minCookingTime != null
    }

    /**
     * Get meal type categories
     */
    companion object {
        val MEAL_TYPE_CATEGORIES = setOf(
            RecipeCategory.BREAKFAST,
            RecipeCategory.LUNCH,
            RecipeCategory.DINNER,
            RecipeCategory.DESSERT,
            RecipeCategory.SNACK,
            RecipeCategory.APPETIZER
        )

        val DIETARY_CATEGORIES = setOf(
            RecipeCategory.VEGETARIAN,
            RecipeCategory.VEGAN,
            RecipeCategory.GLUTEN_FREE,
            RecipeCategory.DAIRY_FREE
        )

        val CUISINE_CATEGORIES = setOf(
            RecipeCategory.ITALIAN,
            RecipeCategory.POLISH,
            RecipeCategory.ASIAN,
            RecipeCategory.MEXICAN,
            RecipeCategory.GREEK,
            RecipeCategory.AMERICAN
        )
    }
}
