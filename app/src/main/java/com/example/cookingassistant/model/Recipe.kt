package com.example.cookingassistant.model

import kotlinx.serialization.Serializable

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

@Serializable
data class Ingredient(
    val name: String,
    val quantity: String,
    val notes: String? = null
)

@Serializable
data class RecipeStep(
    val stepNumber: Int,
    val instruction: String,
    val durationMinutes: Int? = null,
    val mediaItems: List<StepMedia> = emptyList(),
    val tips: String? = null,
    val ingredients: List<Ingredient> = emptyList()
)

@Serializable
data class StepMedia(
    val type: MediaType,
    val uri: String,
    val caption: String? = null,
    val thumbnailUri: String? = null
)

@Serializable
enum class MediaType {
    PHOTO,
    VIDEO
}

@Serializable
enum class Difficulty {
    EASY,
    MEDIUM,
    HARD
}

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

data class RecipeFilters(
    val mealTypes: Set<RecipeCategory> = emptySet(),
    val dietaryPreferences: Set<RecipeCategory> = emptySet(),
    val cuisines: Set<RecipeCategory> = emptySet(),
    val difficulties: Set<Difficulty> = emptySet(),
    val maxCookingTime: Int? = null,
    val minCookingTime: Int? = null
) {
    fun hasActiveFilters(): Boolean {
        return mealTypes.isNotEmpty() ||
                dietaryPreferences.isNotEmpty() ||
                cuisines.isNotEmpty() ||
                difficulties.isNotEmpty() ||
                maxCookingTime != null ||
                minCookingTime != null
    }

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
