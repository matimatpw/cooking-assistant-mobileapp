package com.example.cookingassistant.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cookingassistant.R
import com.example.cookingassistant.model.Difficulty
import com.example.cookingassistant.model.RecipeCategory
import com.example.cookingassistant.model.RecipeFilters
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Bottom sheet for filtering recipes
 *
 * @param currentFilters Current active filters
 * @param onDismiss Called when the sheet is dismissed
 * @param onApplyFilters Called when user applies new filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilters: RecipeFilters,
    onDismiss: () -> Unit,
    onApplyFilters: (RecipeFilters) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Local state for building filters
    var selectedMealTypes by remember { mutableStateOf(currentFilters.mealTypes) }
    var selectedDietary by remember { mutableStateOf(currentFilters.dietaryPreferences) }
    var selectedCuisines by remember { mutableStateOf(currentFilters.cuisines) }
    var selectedDifficulties by remember { mutableStateOf(currentFilters.difficulties) }

    // Cooking time range state
    val defaultMinTime = 5f
    val defaultMaxTime = 480f
    var cookingTimeRange by remember {
        mutableStateOf(
            (currentFilters.minCookingTime?.toFloat() ?: defaultMinTime)..(currentFilters.maxCookingTime?.toFloat() ?: defaultMaxTime)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_recipes),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Meal Type Section
            FilterSection(
                title = stringResource(R.string.filter_meal_type),
                categories = RecipeFilters.MEAL_TYPE_CATEGORIES.toList(),
                selectedCategories = selectedMealTypes,
                onCategoryToggle = { category ->
                    selectedMealTypes = if (category in selectedMealTypes) {
                        selectedMealTypes - category
                    } else {
                        selectedMealTypes + category
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Dietary Preferences Section
            FilterSection(
                title = stringResource(R.string.filter_dietary),
                categories = RecipeFilters.DIETARY_CATEGORIES.toList(),
                selectedCategories = selectedDietary,
                onCategoryToggle = { category ->
                    selectedDietary = if (category in selectedDietary) {
                        selectedDietary - category
                    } else {
                        selectedDietary + category
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Cuisine Section
            FilterSection(
                title = stringResource(R.string.filter_cuisine),
                categories = RecipeFilters.CUISINE_CATEGORIES.toList(),
                selectedCategories = selectedCuisines,
                onCategoryToggle = { category ->
                    selectedCuisines = if (category in selectedCuisines) {
                        selectedCuisines - category
                    } else {
                        selectedCuisines + category
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Difficulty Section
            DifficultyFilterSection(
                selectedDifficulties = selectedDifficulties,
                onDifficultyToggle = { difficulty ->
                    selectedDifficulties = if (difficulty in selectedDifficulties) {
                        selectedDifficulties - difficulty
                    } else {
                        selectedDifficulties + difficulty
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Cooking Time Section
            CookingTimeFilterSection(
                cookingTimeRange = cookingTimeRange,
                onCookingTimeRangeChange = { cookingTimeRange = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Reset all filters
                        selectedMealTypes = emptySet()
                        selectedDietary = emptySet()
                        selectedCuisines = emptySet()
                        selectedDifficulties = emptySet()
                        cookingTimeRange = defaultMinTime..defaultMaxTime
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.clear_filters))
                }

                Button(
                    onClick = {
                        // Auto-detect if time filter should be applied
                        val isTimeFilterActive = cookingTimeRange.start > defaultMinTime || cookingTimeRange.endInclusive < defaultMaxTime

                        val newFilters = RecipeFilters(
                            mealTypes = selectedMealTypes,
                            dietaryPreferences = selectedDietary,
                            cuisines = selectedCuisines,
                            difficulties = selectedDifficulties,
                            minCookingTime = if (isTimeFilterActive) cookingTimeRange.start.roundToInt() else null,
                            maxCookingTime = if (isTimeFilterActive) cookingTimeRange.endInclusive.roundToInt() else null
                        )
                        onApplyFilters(newFilters)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.apply_filters))
                }
            }
        }
    }
}

/**
 * Section for filtering by categories (meal type, dietary, cuisine)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSection(
    title: String,
    categories: List<RecipeCategory>,
    selectedCategories: Set<RecipeCategory>,
    onCategoryToggle: (RecipeCategory) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = category in selectedCategories,
                    onClick = { onCategoryToggle(category) },
                    label = {
                        Text(getCategoryDisplayName(category))
                    }
                )
            }
        }
    }
}

/**
 * Section for filtering by difficulty
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DifficultyFilterSection(
    selectedDifficulties: Set<Difficulty>,
    onDifficultyToggle: (Difficulty) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.filter_difficulty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Difficulty.entries.forEach { difficulty ->
                FilterChip(
                    selected = difficulty in selectedDifficulties,
                    onClick = { onDifficultyToggle(difficulty) },
                    label = {
                        Text(getDifficultyDisplayName(difficulty))
                    }
                )
            }
        }
    }
}

/**
 * Section for filtering by cooking time range with a single range slider
 */
@Composable
fun CookingTimeFilterSection(
    cookingTimeRange: ClosedFloatingPointRange<Float>,
    onCookingTimeRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val defaultMinTime = 5f
    val defaultMaxTime = 480f
    val isFilterActive = cookingTimeRange.start > defaultMinTime || cookingTimeRange.endInclusive < defaultMaxTime

    Column {
        Text(
            text = stringResource(R.string.filter_cooking_time),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Show current range
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.min_label, formatCookingTime(cookingTimeRange.start.roundToInt())),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isFilterActive) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = stringResource(R.string.max_label, formatCookingTime(cookingTimeRange.endInclusive.roundToInt())),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isFilterActive) FontWeight.SemiBold else FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Range slider with both thumbs on one bar
        RangeSlider(
            value = cookingTimeRange,
            onValueChange = onCookingTimeRangeChange,
            valueRange = 5f..480f,
            steps = 30, // Steps every ~15 minutes
            modifier = Modifier.fillMaxWidth()
        )

        // Hint text
        if (!isFilterActive) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.time_filter_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

/**
 * Format cooking time in a human-readable way
 * For times under 60 minutes, shows minutes
 * For times 60+ minutes, shows hours and minutes
 */
@Composable
fun formatCookingTime(minutes: Int): String {
    return if (minutes < 60) {
        stringResource(R.string.minutes_format, minutes)
    } else {
        val hours = minutes / 60
        val mins = minutes % 60
        if (mins == 0) {
            stringResource(R.string.hours_format, hours)
        } else {
            stringResource(R.string.hours_minutes_format, hours, mins)
        }
    }
}

/**
 * Get display name for a recipe category
 */
@Composable
fun getCategoryDisplayName(category: RecipeCategory): String {
    val resourceId = when (category) {
        RecipeCategory.BREAKFAST -> R.string.category_breakfast
        RecipeCategory.LUNCH -> R.string.category_lunch
        RecipeCategory.DINNER -> R.string.category_dinner
        RecipeCategory.DESSERT -> R.string.category_dessert
        RecipeCategory.SNACK -> R.string.category_snack
        RecipeCategory.APPETIZER -> R.string.category_appetizer
        RecipeCategory.VEGETARIAN -> R.string.category_vegetarian
        RecipeCategory.VEGAN -> R.string.category_vegan
        RecipeCategory.GLUTEN_FREE -> R.string.category_gluten_free
        RecipeCategory.DAIRY_FREE -> R.string.category_dairy_free
        RecipeCategory.ITALIAN -> R.string.category_italian
        RecipeCategory.POLISH -> R.string.category_polish
        RecipeCategory.ASIAN -> R.string.category_asian
        RecipeCategory.MEXICAN -> R.string.category_mexican
        RecipeCategory.GREEK -> R.string.category_greek
        RecipeCategory.AMERICAN -> R.string.category_american
        RecipeCategory.QUICK_MEAL -> R.string.category_quick_meal
        RecipeCategory.MEAL_PREP -> R.string.category_meal_prep
        RecipeCategory.PARTY -> R.string.category_party
        RecipeCategory.HOLIDAY -> R.string.category_holiday
    }
    return stringResource(resourceId)
}

/**
 * Get display name for a difficulty level
 */
@Composable
fun getDifficultyDisplayName(difficulty: Difficulty): String {
    val resourceId = when (difficulty) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    }
    return stringResource(resourceId)
}
