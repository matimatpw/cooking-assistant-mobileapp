package com.example.cookingassistant.ui.theme

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cookingassistant.R
import com.example.cookingassistant.model.Difficulty
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.util.LocaleManager
import com.example.cookingassistant.viewmodel.RecipeListState
import com.example.cookingassistant.viewmodel.RecipeViewModel

/**
 * Tab row for switching between Explore and My Recipes tabs
 */
@Composable
fun RecipeListTabs(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Tab(
            selected = selectedTabIndex == 0,
            onClick = { onTabSelected(0) },
            text = { Text(stringResource(R.string.explore_tab)) }
        )
        Tab(
            selected = selectedTabIndex == 1,
            onClick = { onTabSelected(1) },
            text = { Text(stringResource(R.string.my_recipes_tab)) }
        )
    }
}

/**
 * Main screen displaying list of recipes
 * Observes ViewModel state and navigates to detail screen on item click
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    viewModel: RecipeViewModel,
    onRecipeClick: (String) -> Unit,
    onAddRecipe: () -> Unit = {}
) {
    // Collect UI state from ViewModel - automatically updates when state changes
    val state by viewModel.state.collectAsState()
    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val context = LocalContext.current
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cooking_assistant)) },
                actions = {
                    // Filter button with badge if filters are active
                    IconButton(onClick = { showFilterSheet = true }) {
                        if (filters.hasActiveFilters()) {
                            androidx.compose.material3.Badge(
                                containerColor = MaterialTheme.colorScheme.error
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filters)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = stringResource(R.string.filters)
                            )
                        }
                    }
                    IconButton(onClick = { showLanguageMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.language)
                        )
                    }
                    LanguageDropdownMenu(
                        expanded = showLanguageMenu,
                        onDismiss = { showLanguageMenu = false },
                        onLanguageSelected = { languageCode ->
                            showLanguageMenu = false
                            LocaleManager.setLanguage(context, languageCode)
                            // Restart activity to apply language change
                            (context as? Activity)?.recreate()
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRecipe,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Recipe"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row for switching between Explore and My Recipes
            RecipeListTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // Handle different UI states
            when (val currentState = state) {
                is RecipeListState.Loading -> {
                // Show loading spinner on initial load
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is RecipeListState.Success -> {
                // Filter recipes based on selected tab
                val displayedRecipes = viewModel.getRecipesForTab(selectedTabIndex, currentState.recipes)

                // Show recipe list with pull-to-refresh
                PullToRefreshBox(
                    isRefreshing = currentState.isRefreshing,
                    onRefresh = { viewModel.refreshRecipes() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (displayedRecipes.isEmpty()) {
                        // Show tab-specific empty state with fillMaxSize to enable pull-to-refresh
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (selectedTabIndex == 0) {
                                        stringResource(R.string.no_explore_recipes)
                                    } else {
                                        stringResource(R.string.no_custom_recipes)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Show recipe list
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayedRecipes) { recipe ->
                                RecipeListItem(
                                    recipe = recipe,
                                    onClick = { onRecipeClick(recipe.id) }
                                )
                            }
                        }
                    }
                }
            }

            is RecipeListState.Error -> {
                // Show error with cached recipes if available
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Error message at top
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = currentState.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Show cached recipes if available
                    if (currentState.cachedRecipes.isNotEmpty()) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(currentState.cachedRecipes) { recipe ->
                                RecipeListItem(
                                    recipe = recipe,
                                    onClick = { onRecipeClick(recipe.id) }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_recipes_available),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            }
        }

        // Show filter bottom sheet when requested
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilters = filters,
                onDismiss = { showFilterSheet = false },
                onApplyFilters = { newFilters ->
                    viewModel.updateFilters(newFilters)
                    showFilterSheet = false
                }
            )
        }
    }
}

/**
 * Individual recipe item in the list
 * Displays recipe photo, name, description, categories, difficulty, and cooking time
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeListItem(
    recipe: Recipe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Main photo thumbnail
            if (recipe.mainPhotoUri != null) {
                AsyncImage(
                    model = recipe.mainPhotoUri,
                    contentDescription = recipe.name,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Recipe details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Recipe name with difficulty badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Difficulty badge
                    DifficultyBadge(difficulty = recipe.difficulty)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Description preview
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Time and servings info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "⏱️ " + stringResource(R.string.minutes, recipe.cookingTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "🍽️ ${recipe.servings}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Categories as chips
                if (recipe.categories.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recipe.categories.take(3).forEach { category ->
                            CategoryChip(category = category.name)
                        }
                        if (recipe.categories.size > 3) {
                            Text(
                                text = "+${recipe.categories.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays difficulty level as a colored badge
 */
@Composable
fun DifficultyBadge(difficulty: Difficulty) {
    val (color, textResId) = when (difficulty) {
        Difficulty.EASY -> MaterialTheme.colorScheme.tertiary to R.string.difficulty_easy
        Difficulty.MEDIUM -> MaterialTheme.colorScheme.primary to R.string.difficulty_medium
        Difficulty.HARD -> MaterialTheme.colorScheme.error to R.string.difficulty_hard
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(0.dp)
    ) {
        Text(
            text = stringResource(textResId),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Displays a category as a chip
 */
@Composable
fun CategoryChip(category: String) {
    val categoryResId = when (category.uppercase()) {
        "BREAKFAST" -> R.string.category_breakfast
        "LUNCH" -> R.string.category_lunch
        "DINNER" -> R.string.category_dinner
        "DESSERT" -> R.string.category_dessert
        "SNACK" -> R.string.category_snack
        "APPETIZER" -> R.string.category_appetizer
        "VEGETARIAN" -> R.string.category_vegetarian
        "VEGAN" -> R.string.category_vegan
        "GLUTEN_FREE" -> R.string.category_gluten_free
        "DAIRY_FREE" -> R.string.category_dairy_free
        "ITALIAN" -> R.string.category_italian
        "POLISH" -> R.string.category_polish
        "ASIAN" -> R.string.category_asian
        "MEXICAN" -> R.string.category_mexican
        "GREEK" -> R.string.category_greek
        "AMERICAN" -> R.string.category_american
        "QUICK_MEAL" -> R.string.category_quick_meal
        "MEAL_PREP" -> R.string.category_meal_prep
        "PARTY" -> R.string.category_party
        "HOLIDAY" -> R.string.category_holiday
        else -> null
    }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = if (categoryResId != null) {
                stringResource(categoryResId)
            } else {
                category.lowercase().replace("_", " ")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Dropdown menu for language selection
 */
@Composable
fun LanguageDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val currentLanguage = LocaleManager.getCurrentLanguage(context)

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        LocaleManager.getSupportedLanguages().forEach { language ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(language.displayName)
                        if (language.code == currentLanguage) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                },
                onClick = {
                    if (language.code != currentLanguage) {
                        onLanguageSelected(language.code)
                    } else {
                        onDismiss()
                    }
                }
            )
        }
    }
}