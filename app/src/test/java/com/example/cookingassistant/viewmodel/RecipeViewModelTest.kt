package com.example.cookingassistant.viewmodel

import com.example.cookingassistant.model.Difficulty
import com.example.cookingassistant.model.RecipeCategory
import com.example.cookingassistant.model.RecipeFilters
import com.example.cookingassistant.voice.VoiceCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {

    private lateinit var viewModel: RecipeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RecipeViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun recipes_are_loaded_on_initialization() {
        val recipes = viewModel.recipes.value

        assertThat(recipes).isNotEmpty
        assertThat(recipes).hasSize(5)
    }

    @Test
    fun get_recipe_by_id_returns_correct_recipe() {
        val recipe = viewModel.getRecipeById("001")

        assertThat(recipe).isNotNull
        assertThat(recipe?.name).isEqualTo("Spaghetti Carbonara")
    }

    @Test
    fun get_recipe_by_id_returns_null_for_nonexistent_id() {
        val recipe = viewModel.getRecipeById("999")

        assertThat(recipe).isNull()
    }

    @Test
    fun start_cooking_mode_sets_active_recipe_and_resets_step() {
        val recipe = viewModel.getRecipeById("001")!!

        viewModel.startCookingMode(recipe)

        assertThat(viewModel.activeRecipe.value).isEqualTo(recipe)
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun next_step_increments_step_index() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)

        val result = viewModel.nextStep()

        assertThat(result).isTrue()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(1)
    }

    @Test
    fun next_step_returns_false_at_last_step() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)

        // Move to last step
        val lastStepIndex = recipe.steps.size - 1
        viewModel.goToStep(lastStepIndex)

        val result = viewModel.nextStep()

        assertThat(result).isFalse()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(lastStepIndex)
    }

    @Test
    fun previous_step_decrements_step_index() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)
        viewModel.nextStep() // Move to step 1

        val result = viewModel.previousStep()

        assertThat(result).isTrue()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun previous_step_returns_false_at_first_step() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)

        val result = viewModel.previousStep()

        assertThat(result).isFalse()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun go_to_step_sets_correct_step_index() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)

        viewModel.goToStep(3)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(3)
    }

    @Test
    fun go_to_step_ignores_invalid_index() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)

        viewModel.goToStep(999)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun reset_to_first_step_returns_to_beginning() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)
        viewModel.goToStep(3)

        viewModel.resetToFirstStep()

        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun process_voice_command_next_advances_step() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)

        viewModel.processVoiceCommand(VoiceCommand.NEXT)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(1)
    }

    @Test
    fun process_voice_command_previous_goes_back() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)
        viewModel.nextStep()

        viewModel.processVoiceCommand(VoiceCommand.PREVIOUS)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun process_voice_command_start_returns_to_first_step() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)
        viewModel.goToStep(3)

        viewModel.processVoiceCommand(VoiceCommand.START)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun process_voice_command_repeat_stays_on_current_step() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)
        viewModel.goToStep(2)

        viewModel.processVoiceCommand(VoiceCommand.REPEAT)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(2)
    }

    @Test
    fun exit_cooking_mode_clears_active_recipe() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)

        viewModel.exitCookingMode()

        assertThat(viewModel.activeRecipe.value).isNull()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun navigation_through_complete_recipe_flow() {
        val recipe = viewModel.getRecipeById("001")!!
        viewModel.startCookingMode(recipe)

        // Start at step 0
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)

        // Next to step 1
        viewModel.processVoiceCommand(VoiceCommand.NEXT)
        assertThat(viewModel.currentStepIndex.value).isEqualTo(1)

        // Next to step 2
        viewModel.processVoiceCommand(VoiceCommand.NEXT)
        assertThat(viewModel.currentStepIndex.value).isEqualTo(2)

        // Previous back to step 1
        viewModel.processVoiceCommand(VoiceCommand.PREVIOUS)
        assertThat(viewModel.currentStepIndex.value).isEqualTo(1)

        // Start command back to step 0
        viewModel.processVoiceCommand(VoiceCommand.START)
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)

        // Repeat stays at step 0
        viewModel.processVoiceCommand(VoiceCommand.REPEAT)
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    // ========== Filtering Tests ==========

    @Test
    fun filters_are_initially_empty() {
        val filters = viewModel.filters.value

        assertThat(filters.hasActiveFilters()).isFalse()
        assertThat(filters.mealTypes).isEmpty()
        assertThat(filters.dietaryPreferences).isEmpty()
        assertThat(filters.cuisines).isEmpty()
        assertThat(filters.difficulties).isEmpty()
        assertThat(filters.maxCookingTime).isNull()
        assertThat(filters.minCookingTime).isNull()
    }

    @Test
    fun update_filters_changes_filter_state() {
        val newFilters = RecipeFilters(
            mealTypes = setOf(RecipeCategory.BREAKFAST),
            difficulties = setOf(Difficulty.EASY)
        )

        viewModel.updateFilters(newFilters)

        assertThat(viewModel.filters.value).isEqualTo(newFilters)
        assertThat(viewModel.filters.value.hasActiveFilters()).isTrue()
    }

    @Test
    fun clear_filters_resets_to_empty_filters() {
        val newFilters = RecipeFilters(
            mealTypes = setOf(RecipeCategory.BREAKFAST),
            difficulties = setOf(Difficulty.EASY)
        )
        viewModel.updateFilters(newFilters)

        viewModel.clearFilters()

        assertThat(viewModel.filters.value.hasActiveFilters()).isFalse()
    }

    @Test
    fun apply_filters_returns_all_recipes_when_no_filters_active() {
        val recipes = viewModel.recipes.value
        val emptyFilters = RecipeFilters()

        val filtered = viewModel.applyFilters(recipes, emptyFilters)

        assertThat(filtered).hasSize(recipes.size)
        assertThat(filtered).isEqualTo(recipes)
    }

    @Test
    fun apply_filters_by_meal_type_returns_matching_recipes() {
        val recipes = viewModel.recipes.value
        val filters = RecipeFilters(mealTypes = setOf(RecipeCategory.BREAKFAST))

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            assertThat(recipe.categories).contains(RecipeCategory.BREAKFAST)
        }
    }

    @Test
    fun apply_filters_by_difficulty_returns_matching_recipes() {
        val recipes = viewModel.recipes.value
        val filters = RecipeFilters(difficulties = setOf(Difficulty.EASY))

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            assertThat(recipe.difficulty).isEqualTo(Difficulty.EASY)
        }
    }

    @Test
    fun apply_filters_by_cuisine_returns_matching_recipes() {
        val recipes = viewModel.recipes.value
        val filters = RecipeFilters(cuisines = setOf(RecipeCategory.ITALIAN))

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            assertThat(recipe.categories).contains(RecipeCategory.ITALIAN)
        }
    }

    @Test
    fun apply_filters_by_dietary_preference_returns_matching_recipes() {
        val recipes = viewModel.recipes.value
        val filters = RecipeFilters(dietaryPreferences = setOf(RecipeCategory.VEGETARIAN))

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            assertThat(recipe.categories).contains(RecipeCategory.VEGETARIAN)
        }
    }

    @Test
    fun apply_filters_by_max_cooking_time_returns_matching_recipes() {
        val recipes = viewModel.recipes.value
        val maxTime = 15
        val filters = RecipeFilters(maxCookingTime = maxTime)

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            assertThat(recipe.cookingTime).isLessThanOrEqualTo(maxTime)
        }
    }

    @Test
    fun apply_filters_by_min_cooking_time_returns_matching_recipes() {
        val recipes = viewModel.recipes.value
        val minTime = 20
        val filters = RecipeFilters(minCookingTime = minTime)

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            assertThat(recipe.cookingTime).isGreaterThanOrEqualTo(minTime)
        }
    }

    @Test
    fun apply_filters_by_cooking_time_range_returns_matching_recipes() {
        val recipes = viewModel.recipes.value
        val minTime = 10
        val maxTime = 20
        val filters = RecipeFilters(minCookingTime = minTime, maxCookingTime = maxTime)

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            assertThat(recipe.cookingTime).isBetween(minTime, maxTime)
        }
    }

    @Test
    fun apply_multiple_filters_returns_recipes_matching_all_criteria() {
        val recipes = viewModel.recipes.value
        val filters = RecipeFilters(
            difficulties = setOf(Difficulty.EASY),
            maxCookingTime = 30
        )

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            assertThat(recipe.difficulty).isEqualTo(Difficulty.EASY)
            assertThat(recipe.cookingTime).isLessThanOrEqualTo(30)
        }
    }

    @Test
    fun apply_filters_with_no_matching_recipes_returns_empty_list() {
        val recipes = viewModel.recipes.value
        val filters = RecipeFilters(
            mealTypes = setOf(RecipeCategory.BREAKFAST),
            cuisines = setOf(RecipeCategory.ITALIAN),
            difficulties = setOf(Difficulty.HARD)
        )

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isEmpty()
    }

    @Test
    fun get_recipes_for_tab_applies_filters() {
        val recipes = viewModel.recipes.value
        viewModel.updateFilters(RecipeFilters(difficulties = setOf(Difficulty.EASY)))

        val filtered = viewModel.getRecipesForTab(0, recipes)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            assertThat(recipe.difficulty).isEqualTo(Difficulty.EASY)
            assertThat(recipe.isCustom).isFalse()
        }
    }

    @Test
    fun multiple_meal_types_filter_uses_or_logic() {
        val recipes = viewModel.recipes.value
        val filters = RecipeFilters(
            mealTypes = setOf(RecipeCategory.BREAKFAST, RecipeCategory.LUNCH)
        )

        val filtered = viewModel.applyFilters(recipes, filters)

        assertThat(filtered).isNotEmpty
        filtered.forEach { recipe ->
            val hasBreakfast = recipe.categories.contains(RecipeCategory.BREAKFAST)
            val hasLunch = recipe.categories.contains(RecipeCategory.LUNCH)
            assertThat(hasBreakfast || hasLunch).isTrue()
        }
    }

    @Test
    fun multiple_dietary_preferences_filter_uses_and_logic() {
        val recipes = viewModel.recipes.value
        val filters = RecipeFilters(
            dietaryPreferences = setOf(RecipeCategory.VEGETARIAN, RecipeCategory.QUICK_MEAL)
        )

        val filtered = viewModel.applyFilters(recipes, filters)

        // Recipes should have ALL dietary preferences
        filtered.forEach { recipe ->
            assertThat(recipe.categories).contains(RecipeCategory.VEGETARIAN)
            assertThat(recipe.categories).contains(RecipeCategory.QUICK_MEAL)
        }
    }
}
