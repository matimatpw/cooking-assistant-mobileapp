package com.example.cookingassistant.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cookingassistant.R
import com.example.cookingassistant.model.*
import com.example.cookingassistant.ui.theme.components.IngredientInputList
import com.example.cookingassistant.ui.theme.components.MediaPickerButton
import com.example.cookingassistant.ui.theme.components.StepInputList
import com.example.cookingassistant.viewmodel.RecipeViewModel
import java.util.*

/**
 * Add/Edit Recipe Screen
 * Allows creating new recipes or editing existing ones
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRecipeScreen(
    recipeId: String? = null,
    viewModel: RecipeViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    // Load existing recipe if editing
    val existingRecipe = remember(recipeId) {
        recipeId?.let { viewModel.getRecipeById(it) }
    }

    // Form state
    var name by remember { mutableStateOf(existingRecipe?.name ?: "") }
    var description by remember { mutableStateOf(existingRecipe?.description ?: "") }
    var mainPhotoUri by remember { mutableStateOf(existingRecipe?.mainPhotoUri) }
    var prepTime by remember { mutableStateOf(existingRecipe?.prepTime?.toString() ?: "") }
    var cookingTime by remember { mutableStateOf(existingRecipe?.cookingTime?.toString() ?: "") }
    var servings by remember { mutableStateOf(existingRecipe?.servings?.toString() ?: "") }
    var difficulty by remember { mutableStateOf(existingRecipe?.difficulty ?: Difficulty.MEDIUM) }
    var selectedCategories by remember {
        mutableStateOf(existingRecipe?.categories ?: emptySet())
    }
    var tags by remember { mutableStateOf(existingRecipe?.tags?.joinToString(", ") ?: "") }
    var ingredients by remember {
        mutableStateOf(
            existingRecipe?.ingredients ?: listOf(
                Ingredient("", "", null)
            )
        )
    }
    var steps by remember {
        mutableStateOf(
            existingRecipe?.steps ?: listOf(
                RecipeStep(1, "", null, emptyList(), null)
            )
        )
    }

    // Validation state
    var showValidationErrors by remember { mutableStateOf(false) }
    val isValid = remember(name, description, cookingTime, servings, ingredients, steps) {
        name.isNotBlank() &&
                description.isNotBlank() &&
                cookingTime.toIntOrNull() != null &&
                servings.toIntOrNull() != null &&
                ingredients.all { it.name.isNotBlank() && it.quantity.isNotBlank() } &&
                steps.all { it.instruction.isNotBlank() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (recipeId == null) "Add Recipe" else "Edit Recipe")
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (isValid) {
                        saveRecipe(
                            recipeId = recipeId,
                            name = name,
                            description = description,
                            mainPhotoUri = mainPhotoUri,
                            prepTime = prepTime.toIntOrNull() ?: 0,
                            cookingTime = cookingTime.toInt(),
                            servings = servings.toInt(),
                            difficulty = difficulty,
                            categories = selectedCategories,
                            tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            ingredients = ingredients,
                            steps = steps,
                            viewModel = viewModel
                        )
                        onSave()
                    } else {
                        showValidationErrors = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save"
                    )
                },
                text = { Text("Save Recipe") },
                containerColor = if (isValid) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Validation error banner
            if (showValidationErrors && !isValid) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Please fill in all required fields (marked with *)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Basic Information Section
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Recipe name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Recipe Name *") },
                placeholder = { Text("e.g., Chocolate Chip Cookies") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = showValidationErrors && name.isBlank(),
                shape = RoundedCornerShape(8.dp)
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description *") },
                placeholder = { Text("Brief description of the recipe") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                isError = showValidationErrors && description.isBlank(),
                shape = RoundedCornerShape(8.dp)
            )

            // Main photo picker
            MediaPickerButton(
                label = "Main Photo",
                currentMediaUri = mainPhotoUri,
                onMediaSelected = { uri -> mainPhotoUri = uri?.toString() }
            )

            HorizontalDivider()

            // Recipe Details Section
            Text(
                text = "Recipe Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Time and servings row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Prep time
                OutlinedTextField(
                    value = prepTime,
                    onValueChange = { prepTime = it },
                    label = { Text("Prep Time (min)") },
                    placeholder = { Text("10") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )

                // Cooking time
                OutlinedTextField(
                    value = cookingTime,
                    onValueChange = { cookingTime = it },
                    label = { Text("Cook Time (min) *") },
                    placeholder = { Text("30") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && cookingTime.toIntOrNull() == null,
                    shape = RoundedCornerShape(8.dp)
                )

                // Servings
                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it },
                    label = { Text("Servings *") },
                    placeholder = { Text("4") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && servings.toIntOrNull() == null,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Difficulty selector
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Difficulty",
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Difficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff.name) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Categories Section
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            CategorySelector(
                selectedCategories = selectedCategories,
                onCategoriesChange = { selectedCategories = it }
            )

            // Tags
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma-separated)") },
                placeholder = { Text("e.g., quick, vegetarian, italian") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            HorizontalDivider()

            // Ingredients Section
            IngredientInputList(
                ingredients = ingredients,
                onIngredientsChange = { ingredients = it }
            )

            HorizontalDivider()

            // Steps Section
            StepInputList(
                steps = steps,
                onStepsChange = { steps = it }
            )

            // Bottom spacing for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Category selector with chips
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    selectedCategories: Set<RecipeCategory>,
    onCategoriesChange: (Set<RecipeCategory>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Group categories by type
        val categoryGroups = mapOf(
            "Meal Type" to listOf(
                RecipeCategory.BREAKFAST,
                RecipeCategory.LUNCH,
                RecipeCategory.DINNER,
                RecipeCategory.DESSERT,
                RecipeCategory.SNACK,
                RecipeCategory.APPETIZER
            ),
            "Dietary" to listOf(
                RecipeCategory.VEGETARIAN,
                RecipeCategory.VEGAN,
                RecipeCategory.GLUTEN_FREE,
                RecipeCategory.DAIRY_FREE
            ),
            "Cuisine" to listOf(
                RecipeCategory.ITALIAN,
                RecipeCategory.POLISH,
                RecipeCategory.ASIAN,
                RecipeCategory.MEXICAN,
                RecipeCategory.GREEK,
                RecipeCategory.AMERICAN
            ),
            "Occasion" to listOf(
                RecipeCategory.QUICK_MEAL,
                RecipeCategory.MEAL_PREP,
                RecipeCategory.PARTY,
                RecipeCategory.HOLIDAY
            )
        )

        categoryGroups.forEach { (groupName, categories) ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = category in selectedCategories,
                            onClick = {
                                onCategoriesChange(
                                    if (category in selectedCategories) {
                                        selectedCategories - category
                                    } else {
                                        selectedCategories + category
                                    }
                                )
                            },
                            label = {
                                Text(
                                    category.name.lowercase().replace("_", " ")
                                        .replaceFirstChar { it.uppercase() }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Save recipe to repository
 */
private fun saveRecipe(
    recipeId: String?,
    name: String,
    description: String,
    mainPhotoUri: String?,
    prepTime: Int,
    cookingTime: Int,
    servings: Int,
    difficulty: Difficulty,
    categories: Set<RecipeCategory>,
    tags: List<String>,
    ingredients: List<Ingredient>,
    steps: List<RecipeStep>,
    viewModel: RecipeViewModel
) {
    val now = System.currentTimeMillis()
    val existingRecipe = recipeId?.let { viewModel.getRecipeById(it) }

    val recipe = Recipe(
        id = recipeId ?: UUID.randomUUID().toString(),
        name = name,
        description = description,
        mainPhotoUri = mainPhotoUri,
        ingredients = ingredients,
        steps = steps,
        cookingTime = cookingTime,
        prepTime = prepTime,
        servings = servings,
        difficulty = difficulty,
        categories = categories,
        tags = tags,
        createdAt = existingRecipe?.createdAt ?: now,
        updatedAt = now,
        isCustom = true
    )

    // Save via ViewModel (which will use repository)
    if (recipeId == null) {
        // New recipe
        viewModel.saveRecipe(recipe)
    } else {
        // Update existing recipe
        viewModel.updateRecipe(recipe)
    }
}
