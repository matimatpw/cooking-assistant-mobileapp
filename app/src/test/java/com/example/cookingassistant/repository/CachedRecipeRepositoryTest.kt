package com.example.cookingassistant.repository

import com.example.cookingassistant.model.Difficulty
import com.example.cookingassistant.model.Ingredient
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory
import com.example.cookingassistant.model.RecipeStep
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Tests for CachedRecipeRepository focusing on coordination between
 * local and remote data sources, especially refresh behavior.
 *
 * Tests the repository package behavior for caching and refresh logic.
 */
class CachedRecipeRepositoryTest {

    private lateinit var repository: CachedRecipeRepository
    private lateinit var mockLocalDataSource: LocalRecipeDataSource
    private lateinit var mockRemoteDataSource: RemoteRecipeDataSource
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        mockLocalDataSource = mock()
        mockRemoteDataSource = mock()

        repository = CachedRecipeRepository(
            localDataSource = mockLocalDataSource,
            remoteDataSource = mockRemoteDataSource,
            ioDispatcher = testDispatcher
        )
    }

    // ========== Get All Recipes Tests ==========

    @Test
    fun get_all_recipes_returns_from_local_cache() = runTest(testDispatcher) {
        // Given
        val cachedRecipes = listOf(
            createTestRecipe(id = "1", name = "Cached Recipe 1"),
            createTestRecipe(id = "2", name = "Cached Recipe 2")
        )
        whenever(mockLocalDataSource.getAllRecipes()).thenReturn(Result.success(cachedRecipes))

        // When
        val result = repository.getAllRecipes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).hasSize(2)

        // Verify only local source was called, not remote
        verify(mockLocalDataSource).getAllRecipes()
        verifyNoInteractions(mockRemoteDataSource)
    }

    // ========== Refresh Recipes Tests ==========

    @Test
    fun refresh_recipes_fetches_from_remote_api() = runTest(testDispatcher) {
        // Given
        val freshRecipes = listOf(
            createTestRecipe(id = "001", name = "Fresh Recipe 1", isCustom = false),
            createTestRecipe(id = "002", name = "Fresh Recipe 2", isCustom = false)
        )

        whenever(mockLocalDataSource.getAllRecipes()).thenReturn(Result.success(emptyList()))
        whenever(mockRemoteDataSource.fetchAllRecipes()).thenReturn(Result.success(freshRecipes))
        whenever(mockLocalDataSource.saveBundledRecipes(any())).thenReturn(Result.success(Unit))

        // When
        val result = repository.refreshRecipes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertThat(result.isSuccess).isTrue()
        verify(mockRemoteDataSource).fetchAllRecipes()
    }

    @Test
    fun refresh_recipes_saves_fresh_recipes_to_bundled_directory() = runTest(testDispatcher) {
        // Given
        val freshRecipes = listOf(
            createTestRecipe(id = "001", name = "Fresh Recipe", isCustom = false)
        )

        whenever(mockLocalDataSource.getAllRecipes()).thenReturn(Result.success(emptyList()))
        whenever(mockRemoteDataSource.fetchAllRecipes()).thenReturn(Result.success(freshRecipes))
        whenever(mockLocalDataSource.saveBundledRecipes(any())).thenReturn(Result.success(Unit))

        // When
        repository.refreshRecipes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(mockLocalDataSource).saveBundledRecipes(freshRecipes)
    }

    @Test
    fun refresh_recipes_preserves_custom_recipes() = runTest(testDispatcher) {
        // Given - Existing custom recipes in local storage
        val existingRecipes = listOf(
            createTestRecipe(id = "001", name = "Bundled Recipe", isCustom = false),
            createTestRecipe(id = "custom-1", name = "My Custom Recipe 1", isCustom = true),
            createTestRecipe(id = "custom-2", name = "My Custom Recipe 2", isCustom = true)
        )

        // Fresh recipes from API (no custom recipes)
        val freshRecipes = listOf(
            createTestRecipe(id = "001", name = "Fresh Bundled Recipe", isCustom = false),
            createTestRecipe(id = "002", name = "New Bundled Recipe", isCustom = false)
        )

        whenever(mockLocalDataSource.getAllRecipes()).thenReturn(Result.success(existingRecipes))
        whenever(mockRemoteDataSource.fetchAllRecipes()).thenReturn(Result.success(freshRecipes))
        whenever(mockLocalDataSource.saveBundledRecipes(any())).thenReturn(Result.success(Unit))

        // When
        val result = repository.refreshRecipes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Result should include both fresh bundled and preserved custom
        assertThat(result.isSuccess).isTrue()
        val allRecipes = result.getOrNull()

        assertThat(allRecipes).hasSize(4) // 2 fresh bundled + 2 custom

        val bundledRecipes = allRecipes?.filter { !it.isCustom } ?: emptyList()
        val customRecipes = allRecipes?.filter { it.isCustom } ?: emptyList()

        assertThat(bundledRecipes).hasSize(2)
        assertThat(customRecipes).hasSize(2)

        assertThat(customRecipes.map { it.name }).containsExactlyInAnyOrder(
            "My Custom Recipe 1",
            "My Custom Recipe 2"
        )
    }

    @Test
    fun refresh_recipes_replaces_old_bundled_recipes_with_fresh_ones() = runTest(testDispatcher) {
        // Given - Old bundled recipes
        val existingRecipes = listOf(
            createTestRecipe(id = "001", name = "Old Recipe 1", isCustom = false),
            createTestRecipe(id = "002", name = "Old Recipe 2", isCustom = false)
        )

        // Fresh bundled recipes from API
        val freshRecipes = listOf(
            createTestRecipe(id = "001", name = "Updated Recipe 1", isCustom = false),
            createTestRecipe(id = "003", name = "New Recipe 3", isCustom = false)
        )

        whenever(mockLocalDataSource.getAllRecipes()).thenReturn(Result.success(existingRecipes))
        whenever(mockRemoteDataSource.fetchAllRecipes()).thenReturn(Result.success(freshRecipes))
        whenever(mockLocalDataSource.saveBundledRecipes(any())).thenReturn(Result.success(Unit))

        // When
        val result = repository.refreshRecipes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Old bundled recipes should be replaced
        assertThat(result.isSuccess).isTrue()
        verify(mockLocalDataSource).saveBundledRecipes(freshRecipes)

        val returnedRecipes = result.getOrNull()
        assertThat(returnedRecipes?.map { it.name }).containsExactlyInAnyOrder(
            "Updated Recipe 1",
            "New Recipe 3"
        )
    }

    @Test
    fun refresh_recipes_handles_remote_api_failure_gracefully() = runTest(testDispatcher) {
        // Given
        val error = RuntimeException("Network error")
        whenever(mockLocalDataSource.getAllRecipes()).thenReturn(Result.success(emptyList()))
        whenever(mockRemoteDataSource.fetchAllRecipes()).thenReturn(Result.failure(error))

        // When
        val result = repository.refreshRecipes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)

        // Should not attempt to save anything if fetch failed
        verify(mockLocalDataSource, never()).saveBundledRecipes(any())
    }

    @Test
    fun refresh_recipes_continues_even_if_save_to_cache_fails() = runTest(testDispatcher) {
        // Given
        val freshRecipes = listOf(createTestRecipe(id = "001", name = "Fresh"))

        whenever(mockLocalDataSource.getAllRecipes()).thenReturn(Result.success(emptyList()))
        whenever(mockRemoteDataSource.fetchAllRecipes()).thenReturn(Result.success(freshRecipes))
        whenever(mockLocalDataSource.saveBundledRecipes(any()))
            .thenReturn(Result.failure(RuntimeException("Save failed")))

        // When
        val result = repository.refreshRecipes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should still return fresh recipes even if cache save failed
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).hasSize(1)
    }

    // ========== Save Recipe Tests ==========

    @Test
    fun save_recipe_saves_to_local_storage() = runTest(testDispatcher) {
        // Given
        val recipe = createTestRecipe(id = "new-recipe", name = "New Recipe")
        whenever(mockLocalDataSource.saveRecipe(any())).thenReturn(Result.success("new-recipe"))

        // When
        val result = repository.saveRecipe(recipe)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertThat(result.isSuccess).isTrue()
        verify(mockLocalDataSource).saveRecipe(recipe)

        // Should NOT sync to remote (mock API doesn't support this yet)
        verifyNoInteractions(mockRemoteDataSource)
    }

    // ========== Update Recipe Tests ==========

    @Test
    fun update_recipe_updates_local_storage() = runTest(testDispatcher) {
        // Given
        val recipe = createTestRecipe(id = "update-me", name = "Updated Recipe")
        whenever(mockLocalDataSource.updateRecipe(any())).thenReturn(Result.success(Unit))

        // When
        val result = repository.updateRecipe(recipe)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertThat(result.isSuccess).isTrue()
        verify(mockLocalDataSource).updateRecipe(recipe)
    }

    // ========== Delete Recipe Tests ==========

    @Test
    fun delete_recipe_deletes_from_local_storage() = runTest(testDispatcher) {
        // Given
        val recipeId = "delete-me"
        whenever(mockLocalDataSource.deleteRecipe(any())).thenReturn(Result.success(Unit))

        // When
        val result = repository.deleteRecipe(recipeId)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertThat(result.isSuccess).isTrue()
        verify(mockLocalDataSource).deleteRecipe(recipeId)
    }

    // ========== Cache-First Strategy Tests ==========

    @Test
    fun repository_uses_cache_first_strategy_for_read_operations() = runTest(testDispatcher) {
        // Given
        val cachedRecipes = listOf(createTestRecipe(id = "cached", name = "Cached"))
        whenever(mockLocalDataSource.getAllRecipes()).thenReturn(Result.success(cachedRecipes))

        // When - Call getAllRecipes multiple times
        repository.getAllRecipes()
        repository.getAllRecipes()
        repository.getAllRecipes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should only use local cache, never remote
        verify(mockLocalDataSource, times(3)).getAllRecipes()
        verifyNoInteractions(mockRemoteDataSource)
    }

    @Test
    fun refresh_is_explicit_user_action_not_automatic() = runTest(testDispatcher) {
        // Given
        val cachedRecipes = listOf(createTestRecipe(id = "cached", name = "Cached"))
        whenever(mockLocalDataSource.getAllRecipes()).thenReturn(Result.success(cachedRecipes))

        // When - Get all recipes (should NOT automatically refresh)
        repository.getAllRecipes()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Remote API should NOT be called automatically
        verifyNoInteractions(mockRemoteDataSource)
    }

    // ========== Helper Methods ==========

    private fun createTestRecipe(
        id: String,
        name: String,
        description: String = "Test description",
        isCustom: Boolean = true
    ): Recipe {
        return Recipe(
            id = id,
            name = name,
            description = description,
            mainPhotoUri = null,
            ingredients = listOf(Ingredient("test ingredient", "1 cup")),
            steps = listOf(RecipeStep(1, "Test step", 5)),
            cookingTime = 30,
            prepTime = 10,
            servings = 4,
            difficulty = Difficulty.EASY,
            categories = setOf(RecipeCategory.DINNER),
            tags = listOf("test"),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isCustom = isCustom
        )
    }
}
