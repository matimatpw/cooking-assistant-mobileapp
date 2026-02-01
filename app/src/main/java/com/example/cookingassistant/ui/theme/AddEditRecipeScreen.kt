package com.example.cookingassistant.ui.theme

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import com.example.cookingassistant.ui.theme.components.IngredientInputList
import com.example.cookingassistant.ui.theme.components.MediaPickerButton
import com.example.cookingassistant.ui.theme.components.StepInputList
import com.example.cookingassistant.viewmodel.RecipeViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditRecipeScreen(
    recipeId: String? = null,
    viewModel: RecipeViewModel,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val existingRecipe = remember(recipeId) {
        recipeId?.let { viewModel.getRecipeById(it) }
    }

    var showDraftDialog by remember { mutableStateOf(false) }
    var loadedDraft by remember { mutableStateOf<RecipeDraft?>(null) }

    LaunchedEffect(Unit) {
        if (recipeId == null && viewModel.hasDraft()) {
            loadedDraft = viewModel.loadDraft()
            showDraftDialog = true
        }
    }

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

    fun loadDraftIntoForm(draft: RecipeDraft) {
        name = draft.name
        description = draft.description
        mainPhotoUri = draft.mainPhotoUri
        prepTime = draft.prepTime
        cookingTime = draft.cookingTime
        servings = draft.servings
        difficulty = draft.difficulty
        selectedCategories = draft.selectedCategories
        tags = draft.tags
        ingredients = draft.ingredients
        steps = draft.steps
    }

    var showValidationErrors by remember { mutableStateOf(false) }
    val isValid = remember(name, description, cookingTime, servings, ingredients, steps) {
        name.isNotBlank() &&
                description.isNotBlank() &&
                cookingTime.toIntOrNull() != null &&
                servings.toIntOrNull() != null &&
                ingredients.all { it.name.isNotBlank() && it.quantity.isNotBlank() } &&
                steps.all { it.instruction.isNotBlank() }
    }

    var showExitDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = remember(
        name, description, mainPhotoUri, prepTime, cookingTime, servings,
        difficulty, selectedCategories, tags, ingredients, steps, existingRecipe
    ) {
        if (existingRecipe != null) {
            name != existingRecipe.name ||
            description != existingRecipe.description ||
            mainPhotoUri != existingRecipe.mainPhotoUri ||
            prepTime != (existingRecipe.prepTime?.toString() ?: "") ||
            cookingTime != existingRecipe.cookingTime.toString() ||
            servings != existingRecipe.servings.toString() ||
            difficulty != existingRecipe.difficulty ||
            selectedCategories != existingRecipe.categories ||
            tags != existingRecipe.tags.joinToString(", ") ||
            ingredients != existingRecipe.ingredients ||
            steps != existingRecipe.steps
        } else {
            name.isNotBlank() ||
            description.isNotBlank() ||
            mainPhotoUri != null ||
            prepTime.isNotBlank() ||
            cookingTime.isNotBlank() ||
            servings.isNotBlank() ||
            difficulty != Difficulty.MEDIUM ||
            selectedCategories.isNotEmpty() ||
            tags.isNotBlank() ||
            ingredients.size > 1 ||
            (ingredients.size == 1 && (ingredients[0].name.isNotBlank() || ingredients[0].quantity.isNotBlank())) ||
            steps.size > 1 ||
            (steps.size == 1 && steps[0].instruction.isNotBlank())
        }
    }

    BackHandler(enabled = true) {
        if (hasUnsavedChanges) {
            showExitDialog = true
        } else {
            onCancel()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (recipeId == null) stringResource(R.string.add_recipe) else stringResource(R.string.edit_recipe))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showExitDialog = true
                        } else {
                            onCancel()
                        }
                    }) {
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
                        viewModel.clearDraft()
                        onSave()
                    } else {
                        showValidationErrors = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = stringResource(R.string.save)
                    )
                },
                text = { Text(stringResource(R.string.save_recipe)) },
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
            if (showValidationErrors && !isValid) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = stringResource(R.string.validation_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.basic_information),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.recipe_name)) },
                placeholder = { Text(stringResource(R.string.recipe_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = showValidationErrors && name.isBlank(),
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description)) },
                placeholder = { Text(stringResource(R.string.description_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                isError = showValidationErrors && description.isBlank(),
                shape = RoundedCornerShape(8.dp)
            )

            MediaPickerButton(
                label = stringResource(R.string.main_photo),
                currentMediaUri = mainPhotoUri,
                onMediaSelected = { uri -> mainPhotoUri = uri?.toString() }
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.recipe_details),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = prepTime,
                    onValueChange = { prepTime = it },
                    label = { Text(stringResource(R.string.prep_time)) },
                    placeholder = { Text(stringResource(R.string.prep_time_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = cookingTime,
                    onValueChange = { cookingTime = it },
                    label = { Text(stringResource(R.string.cook_time)) },
                    placeholder = { Text(stringResource(R.string.cook_time_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && cookingTime.toIntOrNull() == null,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it },
                    label = { Text(stringResource(R.string.servings)) },
                    placeholder = { Text(stringResource(R.string.servings_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showValidationErrors && servings.toIntOrNull() == null,
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.difficulty),
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Difficulty.entries.forEach { diff ->
                        val difficultyLabel = when (diff) {
                            Difficulty.EASY -> stringResource(R.string.difficulty_easy)
                            Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
                            Difficulty.HARD -> stringResource(R.string.difficulty_hard)
                        }
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(difficultyLabel) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.categories),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            CategorySelector(
                selectedCategories = selectedCategories,
                onCategoriesChange = { selectedCategories = it }
            )

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text(stringResource(R.string.tags)) },
                placeholder = { Text(stringResource(R.string.tags_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            HorizontalDivider()

            IngredientInputList(
                ingredients = ingredients,
                onIngredientsChange = { ingredients = it }
            )

            HorizontalDivider()

            StepInputList(
                steps = steps,
                onStepsChange = { steps = it }
            )

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(text = stringResource(R.string.unsaved_changes_title))
            },
            text = {
                Text(text = stringResource(R.string.unsaved_changes_message))
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (recipeId != null) {
                        Button(
                            onClick = {
                                showExitDialog = false
                                onCancel()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.discard_and_exit))
                        }

                        OutlinedButton(
                            onClick = {
                                showExitDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.stay_and_continue))
                        }
                    } else {
                        Button(
                            onClick = {
                                showExitDialog = false
                                val draft = RecipeDraft(
                                    recipeId = recipeId,
                                    name = name,
                                    description = description,
                                    mainPhotoUri = mainPhotoUri,
                                    prepTime = prepTime,
                                    cookingTime = cookingTime,
                                    servings = servings,
                                    difficulty = difficulty,
                                    selectedCategories = selectedCategories,
                                    tags = tags,
                                    ingredients = ingredients,
                                    steps = steps
                                )
                                viewModel.saveDraft(draft)
                                onCancel()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.save_draft_and_exit))
                        }

                        OutlinedButton(
                            onClick = {
                                showExitDialog = false
                                viewModel.clearDraft()
                                onCancel()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.discard_and_exit))
                        }

                        TextButton(
                            onClick = {
                                showExitDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.stay_and_continue))
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showDraftDialog && loadedDraft != null) {
        AlertDialog(
            onDismissRequest = {
            },
            title = {
                Text(text = stringResource(R.string.draft_found_title))
            },
            text = {
                Text(text = stringResource(R.string.draft_found_message))
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            loadDraftIntoForm(loadedDraft!!)
                            showDraftDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.continue_draft))
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.clearDraft()
                            showDraftDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.start_fresh))
                    }
                }
            },
            dismissButton = {}
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    selectedCategories: Set<RecipeCategory>,
    onCategoriesChange: (Set<RecipeCategory>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val categoryGroups = listOf(
            stringResource(R.string.category_meal_type) to listOf(
                RecipeCategory.BREAKFAST,
                RecipeCategory.LUNCH,
                RecipeCategory.DINNER,
                RecipeCategory.DESSERT,
                RecipeCategory.SNACK,
                RecipeCategory.APPETIZER
            ),
            stringResource(R.string.category_dietary) to listOf(
                RecipeCategory.VEGETARIAN,
                RecipeCategory.VEGAN,
                RecipeCategory.GLUTEN_FREE,
                RecipeCategory.DAIRY_FREE
            ),
            stringResource(R.string.category_cuisine) to listOf(
                RecipeCategory.ITALIAN,
                RecipeCategory.POLISH,
                RecipeCategory.ASIAN,
                RecipeCategory.MEXICAN,
                RecipeCategory.GREEK,
                RecipeCategory.AMERICAN
            ),
            stringResource(R.string.category_occasion) to listOf(
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
                        val categoryLabel = getCategoryLabel(category)
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
                                Text(categoryLabel)
                            }
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun getCategoryLabel(category: RecipeCategory): String {
    return when (category) {
        RecipeCategory.BREAKFAST -> stringResource(R.string.category_breakfast)
        RecipeCategory.LUNCH -> stringResource(R.string.category_lunch)
        RecipeCategory.DINNER -> stringResource(R.string.category_dinner)
        RecipeCategory.DESSERT -> stringResource(R.string.category_dessert)
        RecipeCategory.SNACK -> stringResource(R.string.category_snack)
        RecipeCategory.APPETIZER -> stringResource(R.string.category_appetizer)
        RecipeCategory.VEGETARIAN -> stringResource(R.string.category_vegetarian)
        RecipeCategory.VEGAN -> stringResource(R.string.category_vegan)
        RecipeCategory.GLUTEN_FREE -> stringResource(R.string.category_gluten_free)
        RecipeCategory.DAIRY_FREE -> stringResource(R.string.category_dairy_free)
        RecipeCategory.ITALIAN -> stringResource(R.string.category_italian)
        RecipeCategory.POLISH -> stringResource(R.string.category_polish)
        RecipeCategory.ASIAN -> stringResource(R.string.category_asian)
        RecipeCategory.MEXICAN -> stringResource(R.string.category_mexican)
        RecipeCategory.GREEK -> stringResource(R.string.category_greek)
        RecipeCategory.AMERICAN -> stringResource(R.string.category_american)
        RecipeCategory.QUICK_MEAL -> stringResource(R.string.category_quick_meal)
        RecipeCategory.MEAL_PREP -> stringResource(R.string.category_meal_prep)
        RecipeCategory.PARTY -> stringResource(R.string.category_party)
        RecipeCategory.HOLIDAY -> stringResource(R.string.category_holiday)
    }
}
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

    if (recipeId == null) {
        viewModel.saveRecipe(recipe)
    } else {
        viewModel.updateRecipe(recipe)
    }
}
