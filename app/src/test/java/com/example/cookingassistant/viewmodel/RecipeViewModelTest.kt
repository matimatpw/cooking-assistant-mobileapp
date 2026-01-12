package com.example.cookingassistant.viewmodel

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

/**
 * Unit tests for RecipeViewModel
 * Demonstrates how to test step navigation and voice command processing without running the app
 */
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
        val recipe = viewModel.getRecipeById(1)

        assertThat(recipe).isNotNull
        assertThat(recipe?.name).isEqualTo("Spaghetti Carbonara")
    }

    @Test
    fun get_recipe_by_id_returns_null_for_nonexistent_id() {
        val recipe = viewModel.getRecipeById(999)

        assertThat(recipe).isNull()
    }

    @Test
    fun start_cooking_mode_sets_active_recipe_and_resets_step() {
        val recipe = viewModel.getRecipeById(1)!!

        viewModel.startCookingMode(recipe)

        assertThat(viewModel.activeRecipe.value).isEqualTo(recipe)
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun next_step_increments_step_index() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)

        val result = viewModel.nextStep()

        assertThat(result).isTrue()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(1)
    }

    @Test
    fun next_step_returns_false_at_last_step() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)

        // Move to last step
        val lastStepIndex = recipe.instructions.size - 1
        viewModel.goToStep(lastStepIndex)

        val result = viewModel.nextStep()

        assertThat(result).isFalse()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(lastStepIndex)
    }

    @Test
    fun previous_step_decrements_step_index() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)
        viewModel.nextStep() // Move to step 1

        val result = viewModel.previousStep()

        assertThat(result).isTrue()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun previous_step_returns_false_at_first_step() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)

        val result = viewModel.previousStep()

        assertThat(result).isFalse()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun go_to_step_sets_correct_step_index() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)

        viewModel.goToStep(3)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(3)
    }

    @Test
    fun go_to_step_ignores_invalid_index() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)

        viewModel.goToStep(999)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun reset_to_first_step_returns_to_beginning() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)
        viewModel.goToStep(3)

        viewModel.resetToFirstStep()

        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun process_voice_command_next_advances_step() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)

        viewModel.processVoiceCommand(VoiceCommand.NEXT)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(1)
    }

    @Test
    fun process_voice_command_previous_goes_back() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)
        viewModel.nextStep()

        viewModel.processVoiceCommand(VoiceCommand.PREVIOUS)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun process_voice_command_start_returns_to_first_step() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)
        viewModel.goToStep(3)

        viewModel.processVoiceCommand(VoiceCommand.START)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun process_voice_command_repeat_stays_on_current_step() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)
        viewModel.goToStep(2)

        viewModel.processVoiceCommand(VoiceCommand.REPEAT)

        assertThat(viewModel.currentStepIndex.value).isEqualTo(2)
    }

    @Test
    fun exit_cooking_mode_clears_active_recipe() {
        val recipe = viewModel.getRecipeById(1)!!
        viewModel.startCookingMode(recipe)

        viewModel.exitCookingMode()

        assertThat(viewModel.activeRecipe.value).isNull()
        assertThat(viewModel.currentStepIndex.value).isEqualTo(0)
    }

    @Test
    fun navigation_through_complete_recipe_flow() {
        val recipe = viewModel.getRecipeById(1)!!
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
}
