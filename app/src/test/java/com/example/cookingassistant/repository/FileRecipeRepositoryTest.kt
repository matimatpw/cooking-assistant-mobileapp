package com.example.cookingassistant.repository

import android.content.Context
import com.example.cookingassistant.model.Difficulty
import com.example.cookingassistant.model.Ingredient
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.RecipeCategory
import com.example.cookingassistant.model.RecipeStep
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileRecipeRepositoryTest {

    private lateinit var repository: FileRecipeRepository
    private lateinit var context: Context
    private lateinit var testRecipesDir: File
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        testRecipesDir = File(context.filesDir, "recipes_test")

        if (testRecipesDir.exists()) {
            testRecipesDir.deleteRecursively()
        }

        repository = FileRecipeRepository(context, testDispatcher, "recipes_test")
    }

    @After
    fun teardown() {
        if (testRecipesDir.exists()) {
            testRecipesDir.deleteRecursively()
        }
    }

    @Test
    fun save_recipe_creates_file_and_returns_id() = runTest(testDispatcher) {
        val recipe = createTestRecipe(id = "")

        val result = repository.saveRecipe(recipe)

        assertThat(result.isSuccess).isTrue()
        val recipeId = result.getOrNull()
        assertThat(recipeId).isNotNull()
        assertThat(recipeId).isNotEmpty()
    }

    @Test
    fun save_recipe_with_id_uses_provided_id() = runTest(testDispatcher) {
        val expectedId = "test-recipe-001"
        val recipe = createTestRecipe(id = expectedId)

        val result = repository.saveRecipe(recipe)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(expectedId)
    }

    @Test
    fun get_recipe_by_id_returns_saved_recipe() = runTest(testDispatcher) {
        val recipe = createTestRecipe(id = "test-001")
        repository.saveRecipe(recipe)

        val result = repository.getRecipeById("test-001")

        assertThat(result.isSuccess).isTrue()
        val loadedRecipe = result.getOrNull()
        assertThat(loadedRecipe).isNotNull
        assertThat(loadedRecipe?.id).isEqualTo("test-001")
        assertThat(loadedRecipe?.name).isEqualTo("Test Recipe")
    }

    @Test
    fun get_recipe_by_id_returns_null_when_not_found() = runTest(testDispatcher) {
        val result = repository.getRecipeById("nonexistent")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun get_all_recipes_returns_empty_list_initially() = runTest(testDispatcher) {
        val result = repository.getAllRecipes()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun get_all_recipes_returns_all_saved_recipes() = runTest(testDispatcher) {
        val recipe1 = createTestRecipe(id = "001", name = "Recipe 1")
        val recipe2 = createTestRecipe(id = "002", name = "Recipe 2")
        val recipe3 = createTestRecipe(id = "003", name = "Recipe 3")

        repository.saveRecipe(recipe1)
        repository.saveRecipe(recipe2)
        repository.saveRecipe(recipe3)

        val result = repository.getAllRecipes()

        assertThat(result.isSuccess).isTrue()
        val recipes = result.getOrNull()
        assertThat(recipes).hasSize(3)
        assertThat(recipes?.map { it.name })
            .containsExactlyInAnyOrder("Recipe 1", "Recipe 2", "Recipe 3")
    }

    @Test
    fun update_recipe_modifies_existing_recipe() = runTest(testDispatcher) {
        val originalRecipe = createTestRecipe(id = "test-001", name = "Original Name")
        repository.saveRecipe(originalRecipe)

        val updatedRecipe = originalRecipe.copy(name = "Updated Name")
        val updateResult = repository.updateRecipe(updatedRecipe)

        assertThat(updateResult.isSuccess).isTrue()

        val loadedRecipe = repository.getRecipeById("test-001").getOrNull()
        assertThat(loadedRecipe?.name).isEqualTo("Updated Name")
    }

    @Test
    fun update_recipe_fails_when_recipe_does_not_exist() = runTest(testDispatcher) {
        val recipe = createTestRecipe(id = "nonexistent")

        val result = repository.updateRecipe(recipe)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun delete_recipe_removes_recipe_from_repository() = runTest(testDispatcher) {
        val recipe = createTestRecipe(id = "test-001")
        repository.saveRecipe(recipe)

        val deleteResult = repository.deleteRecipe("test-001")

        assertThat(deleteResult.isSuccess).isTrue()

        val recipes = repository.getAllRecipes().getOrNull()
        assertThat(recipes).isEmpty()

        val deletedRecipe = repository.getRecipeById("test-001").getOrNull()
        assertThat(deletedRecipe).isNull()
    }

    @Test
    fun delete_recipe_succeeds_even_when_recipe_does_not_exist() = runTest(testDispatcher) {
        val result = repository.deleteRecipe("nonexistent")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun get_recipes_by_category_filters_correctly() = runTest(testDispatcher) {
        val italianRecipe = createTestRecipe(
            id = "001",
            name = "Pasta",
            categories = setOf(RecipeCategory.ITALIAN, RecipeCategory.LUNCH)
        )
        val polishRecipe = createTestRecipe(
            id = "002",
            name = "Pierogi",
            categories = setOf(RecipeCategory.POLISH, RecipeCategory.DINNER)
        )
        val mexicanRecipe = createTestRecipe(
            id = "003",
            name = "Tacos",
            categories = setOf(RecipeCategory.MEXICAN, RecipeCategory.QUICK_MEAL)
        )

        repository.saveRecipe(italianRecipe)
        repository.saveRecipe(polishRecipe)
        repository.saveRecipe(mexicanRecipe)

        val result = repository.getRecipesByCategory(RecipeCategory.ITALIAN)

        assertThat(result.isSuccess).isTrue()
        val recipes = result.getOrNull()
        assertThat(recipes).hasSize(1)
        assertThat(recipes?.first()?.name).isEqualTo("Pasta")
    }

    @Test
    fun get_recipes_by_category_returns_multiple_matches() = runTest(testDispatcher) {
        val lunchRecipe1 = createTestRecipe(
            id = "001",
            name = "Sandwich",
            categories = setOf(RecipeCategory.LUNCH, RecipeCategory.QUICK_MEAL)
        )
        val lunchRecipe2 = createTestRecipe(
            id = "002",
            name = "Salad",
            categories = setOf(RecipeCategory.LUNCH, RecipeCategory.VEGETARIAN)
        )
        val dinnerRecipe = createTestRecipe(
            id = "003",
            name = "Steak",
            categories = setOf(RecipeCategory.DINNER)
        )

        repository.saveRecipe(lunchRecipe1)
        repository.saveRecipe(lunchRecipe2)
        repository.saveRecipe(dinnerRecipe)

        val result = repository.getRecipesByCategory(RecipeCategory.LUNCH)

        assertThat(result.isSuccess).isTrue()
        val recipes = result.getOrNull()
        assertThat(recipes).hasSize(2)
        assertThat(recipes?.map { it.name })
            .containsExactlyInAnyOrder("Sandwich", "Salad")
    }

    @Test
    fun search_recipes_finds_by_name() = runTest(testDispatcher) {
        val recipe1 = createTestRecipe(id = "001", name = "Chocolate Cake")
        val recipe2 = createTestRecipe(id = "002", name = "Vanilla Ice Cream")
        val recipe3 = createTestRecipe(id = "003", name = "Chocolate Cookies")

        repository.saveRecipe(recipe1)
        repository.saveRecipe(recipe2)
        repository.saveRecipe(recipe3)

        val result = repository.searchRecipes("chocolate")

        assertThat(result.isSuccess).isTrue()
        val recipes = result.getOrNull()
        assertThat(recipes).hasSize(2)
        assertThat(recipes?.map { it.name })
            .containsExactlyInAnyOrder("Chocolate Cake", "Chocolate Cookies")
    }

    @Test
    fun search_recipes_finds_by_description() = runTest(testDispatcher) {
        val recipe1 = createTestRecipe(
            id = "001",
            name = "Pasta",
            description = "Quick and easy weeknight dinner"
        )
        val recipe2 = createTestRecipe(
            id = "002",
            name = "Salad",
            description = "Healthy lunch option"
        )

        repository.saveRecipe(recipe1)
        repository.saveRecipe(recipe2)

        val result = repository.searchRecipes("quick")

        assertThat(result.isSuccess).isTrue()
        val recipes = result.getOrNull()
        assertThat(recipes).hasSize(1)
        assertThat(recipes?.first()?.name).isEqualTo("Pasta")
    }

    @Test
    fun search_recipes_finds_by_ingredient_name() = runTest(testDispatcher) {
        val recipe1 = createTestRecipe(
            id = "001",
            name = "Pasta Carbonara",
            ingredients = listOf(
                Ingredient("pasta", "400g"),
                Ingredient("eggs", "3"),
                Ingredient("bacon", "200g")
            )
        )
        val recipe2 = createTestRecipe(
            id = "002",
            name = "Fried Rice",
            ingredients = listOf(
                Ingredient("rice", "2 cups"),
                Ingredient("eggs", "2"),
                Ingredient("soy sauce", "2 tbsp")
            )
        )

        repository.saveRecipe(recipe1)
        repository.saveRecipe(recipe2)

        val result = repository.searchRecipes("bacon")

        assertThat(result.isSuccess).isTrue()
        val recipes = result.getOrNull()
        assertThat(recipes).hasSize(1)
        assertThat(recipes?.first()?.name).isEqualTo("Pasta Carbonara")
    }

    @Test
    fun search_recipes_is_case_insensitive() = runTest(testDispatcher) {
        val recipe = createTestRecipe(id = "001", name = "Spaghetti Bolognese")
        repository.saveRecipe(recipe)

        val result = repository.searchRecipes("SPAGHETTI")

        assertThat(result.isSuccess).isTrue()
        val recipes = result.getOrNull()
        assertThat(recipes).hasSize(1)
    }

    @Test
    fun search_recipes_returns_empty_list_when_no_matches() = runTest(testDispatcher) {
        val recipe = createTestRecipe(id = "001", name = "Pasta")
        repository.saveRecipe(recipe)

        val result = repository.searchRecipes("pizza")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun repository_persists_recipes_across_instances() = runTest(testDispatcher) {
        val recipe = createTestRecipe(id = "test-001", name = "Persistent Recipe")
        repository.saveRecipe(recipe)

        // Create new repository instance with same context
        val newRepository = FileRecipeRepository(context, testDispatcher, "recipes_test")
        val result = newRepository.getRecipeById("test-001")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.name).isEqualTo("Persistent Recipe")
    }

    // Helper method to create test recipes
    private fun createTestRecipe(
        id: String = "",
        name: String = "Test Recipe",
        description: String = "Test description",
        ingredients: List<Ingredient> = listOf(
            Ingredient("ingredient 1", "100g"),
            Ingredient("ingredient 2", "2 cups")
        ),
        categories: Set<RecipeCategory> = setOf(RecipeCategory.LUNCH)
    ): Recipe {
        return Recipe(
            id = id,
            name = name,
            description = description,
            mainPhotoUri = null,
            ingredients = ingredients,
            steps = listOf(
                RecipeStep(1, "Step 1 instruction"),
                RecipeStep(2, "Step 2 instruction")
            ),
            cookingTime = 30,
            prepTime = 10,
            servings = 4,
            difficulty = Difficulty.MEDIUM,
            categories = categories,
            tags = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isCustom = true
        )
    }

    // ========== Bundled Recipe Saving Tests ==========

    @Test
    fun save_bundled_recipes_creates_files_in_bundled_directory() = runTest(testDispatcher) {
        // Given
        val recipes = listOf(
            createTestRecipe(id = "001", name = "Bundled Recipe 1"),
            createTestRecipe(id = "002", name = "Bundled Recipe 2"),
            createTestRecipe(id = "003", name = "Bundled Recipe 3")
        )

        // When
        val result = repository.saveBundledRecipes(recipes)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertThat(result.isSuccess).isTrue()

        val bundledDir = File(testRecipesDir, "bundled")
        assertThat(File(bundledDir, "recipe_001.json").exists()).isTrue()
        assertThat(File(bundledDir, "recipe_002.json").exists()).isTrue()
        assertThat(File(bundledDir, "recipe_003.json").exists()).isTrue()
    }

    @Test
    fun save_bundled_recipes_marks_all_recipes_as_not_custom() = runTest(testDispatcher) {
        // Given
        val recipes = listOf(
            createTestRecipe(id = "bundled-1", name = "Bundled 1"),
            createTestRecipe(id = "bundled-2", name = "Bundled 2")
        )

        // When
        repository.saveBundledRecipes(recipes)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val recipe1 = repository.getRecipeById("bundled-1").getOrNull()
        val recipe2 = repository.getRecipeById("bundled-2").getOrNull()

        assertThat(recipe1?.isCustom).isFalse()
        assertThat(recipe2?.isCustom).isFalse()
    }

    @Test
    fun save_bundled_recipes_clears_old_bundled_recipes_before_saving_new_ones() = runTest(testDispatcher) {
        // Given - First save some bundled recipes
        val oldRecipes = listOf(
            createTestRecipe(id = "old-1", name = "Old Recipe 1"),
            createTestRecipe(id = "old-2", name = "Old Recipe 2")
        )
        repository.saveBundledRecipes(oldRecipes)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Save new bundled recipes
        val newRecipes = listOf(
            createTestRecipe(id = "new-1", name = "New Recipe 1"),
            createTestRecipe(id = "new-2", name = "New Recipe 2")
        )
        repository.saveBundledRecipes(newRecipes)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Old recipes should be deleted, new ones should exist
        val bundledDir = File(testRecipesDir, "bundled")
        assertThat(File(bundledDir, "recipe_old-1.json").exists()).isFalse()
        assertThat(File(bundledDir, "recipe_old-2.json").exists()).isFalse()
        assertThat(File(bundledDir, "recipe_new-1.json").exists()).isTrue()
        assertThat(File(bundledDir, "recipe_new-2.json").exists()).isTrue()

        // And - Only new recipes should be retrievable
        assertThat(repository.getRecipeById("old-1").getOrNull()).isNull()
        assertThat(repository.getRecipeById("new-1").getOrNull()).isNotNull()
    }

    @Test
    fun save_bundled_recipes_does_not_delete_custom_recipes() = runTest(testDispatcher) {
        // Given - Save a custom recipe
        val customRecipe = createTestRecipe(id = "my-custom", name = "My Custom Recipe")
        repository.saveRecipe(customRecipe)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Save bundled recipes
        val bundledRecipes = listOf(
            createTestRecipe(id = "bundled-1", name = "Bundled Recipe")
        )
        repository.saveBundledRecipes(bundledRecipes)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Custom recipe should still exist
        val customDir = File(testRecipesDir, "custom")
        assertThat(File(customDir, "recipe_my-custom.json").exists()).isTrue()

        val retrievedCustom = repository.getRecipeById("my-custom").getOrNull()
        assertThat(retrievedCustom).isNotNull()
        assertThat(retrievedCustom?.name).isEqualTo("My Custom Recipe")
        assertThat(retrievedCustom?.isCustom).isTrue()
    }

    @Test
    fun save_bundled_recipes_generates_sequential_ids_for_recipes_without_ids() = runTest(testDispatcher) {
        // Given - Recipes without IDs
        val recipes = listOf(
            createTestRecipe(id = "", name = "No ID 1"),
            createTestRecipe(id = "", name = "No ID 2"),
            createTestRecipe(id = "", name = "No ID 3")
        )

        // When
        repository.saveBundledRecipes(recipes)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Should generate sequential IDs like "001", "002", "003"
        val bundledDir = File(testRecipesDir, "bundled")
        assertThat(File(bundledDir, "recipe_001.json").exists()).isTrue()
        assertThat(File(bundledDir, "recipe_002.json").exists()).isTrue()
        assertThat(File(bundledDir, "recipe_003.json").exists()).isTrue()
    }

    // ========== Custom vs Bundled Separation Tests ==========

    @Test
    fun custom_recipes_are_stored_separately_from_bundled_recipes() = runTest(testDispatcher) {
        // Given
        val customRecipe = createTestRecipe(id = "custom-1", name = "My Recipe")
        val bundledRecipes = listOf(createTestRecipe(id = "001", name = "Bundled Recipe"))

        // When
        repository.saveRecipe(customRecipe)
        repository.saveBundledRecipes(bundledRecipes)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Different directories
        val customDir = File(testRecipesDir, "custom")
        val bundledDir = File(testRecipesDir, "bundled")

        assertThat(File(customDir, "recipe_custom-1.json").exists()).isTrue()
        assertThat(File(bundledDir, "recipe_001.json").exists()).isTrue()
    }

    @Test
    fun get_all_recipes_returns_both_bundled_and_custom_recipes() = runTest(testDispatcher) {
        // Given - Save bundled recipes
        val bundledRecipes = listOf(
            createTestRecipe(id = "001", name = "Bundled 1"),
            createTestRecipe(id = "002", name = "Bundled 2")
        )
        repository.saveBundledRecipes(bundledRecipes)
        testDispatcher.scheduler.advanceUntilIdle()

        // And - Save custom recipes
        repository.saveRecipe(createTestRecipe(id = "custom-1", name = "Custom 1"))
        repository.saveRecipe(createTestRecipe(id = "custom-2", name = "Custom 2"))
        repository.saveRecipe(createTestRecipe(id = "custom-3", name = "Custom 3"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        val allRecipes = repository.getAllRecipes().getOrThrow()

        // Then - Should have all recipes
        assertThat(allRecipes).hasSize(5)

        val bundledCount = allRecipes.count { !it.isCustom }
        val customCount = allRecipes.count { it.isCustom }

        assertThat(bundledCount).isEqualTo(2)
        assertThat(customCount).isEqualTo(3)
    }

    @Test
    fun custom_recipe_flag_is_preserved_after_save_and_load() = runTest(testDispatcher) {
        // Given - Save a custom recipe
        val customRecipe = createTestRecipe(id = "preserve-flag", name = "Custom Recipe")
        repository.saveRecipe(customRecipe)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Load the recipe
        val loadedRecipe = repository.getRecipeById("preserve-flag").getOrNull()

        // Then - isCustom flag should be true
        assertThat(loadedRecipe?.isCustom).isTrue()
    }

    @Test
    fun bundled_recipe_flag_is_preserved_after_save_and_load() = runTest(testDispatcher) {
        // Given - Save bundled recipes
        val bundledRecipes = listOf(createTestRecipe(id = "flag-test", name = "Bundled"))
        repository.saveBundledRecipes(bundledRecipes)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Load the recipe
        val loadedRecipe = repository.getRecipeById("flag-test").getOrNull()

        // Then - isCustom flag should be false
        assertThat(loadedRecipe?.isCustom).isFalse()
    }

    @Test
    fun index_correctly_tracks_bundled_and_custom_recipes() = runTest(testDispatcher) {
        // Given
        repository.saveBundledRecipes(listOf(createTestRecipe(id = "001", name = "Bundled")))
        repository.saveRecipe(createTestRecipe(id = "custom-1", name = "Custom"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Read index file
        val indexFile = File(testRecipesDir, "recipes_index.json")
        val indexContent = indexFile.readText()

        // Then - Index should contain correct metadata
        assertThat(indexContent).contains("\"id\":\"001\"")
        assertThat(indexContent).contains("\"isCustom\":false")
        assertThat(indexContent).contains("bundled/recipe_001.json")

        assertThat(indexContent).contains("\"id\":\"custom-1\"")
        assertThat(indexContent).contains("\"isCustom\":true")
        assertThat(indexContent).contains("custom/recipe_custom-1.json")
    }

    // ========== Recipe Persistence Across Repository Instances ==========

    @Test
    fun custom_recipes_persist_across_repository_instances() = runTest(testDispatcher) {
        // Given - Save custom recipe with first instance
        val recipe = createTestRecipe(id = "persist-custom", name = "Persistent Custom")
        repository.saveRecipe(recipe)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Create new repository instance
        val newRepository = FileRecipeRepository(context, StandardTestDispatcher(), "recipes_test")
        val loadedRecipe = newRepository.getRecipeById("persist-custom").getOrNull()

        // Then
        assertThat(loadedRecipe).isNotNull()
        assertThat(loadedRecipe?.name).isEqualTo("Persistent Custom")
        assertThat(loadedRecipe?.isCustom).isTrue()
    }

    @Test
    fun bundled_recipes_persist_across_repository_instances() = runTest(testDispatcher) {
        // Given - Save bundled recipes with first instance
        repository.saveBundledRecipes(listOf(
            createTestRecipe(id = "persist-001", name = "Persistent Bundled")
        ))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Create new repository instance
        val newRepository = FileRecipeRepository(context, StandardTestDispatcher(), "recipes_test")
        val loadedRecipe = newRepository.getRecipeById("persist-001").getOrNull()

        // Then
        assertThat(loadedRecipe).isNotNull()
        assertThat(loadedRecipe?.name).isEqualTo("Persistent Bundled")
        assertThat(loadedRecipe?.isCustom).isFalse()
    }
}

