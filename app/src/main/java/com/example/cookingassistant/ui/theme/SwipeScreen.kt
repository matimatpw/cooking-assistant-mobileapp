package com.example.cookingassistant.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cookingassistant.model.SwipeType
import com.example.cookingassistant.repository.CachedRecipeRepository
import com.example.cookingassistant.repository.SwipeRepository
import com.example.cookingassistant.ui.theme.components.SwipeableRecipeCard
import com.example.cookingassistant.viewmodel.SwipeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    recipeRepository: CachedRecipeRepository,
    swipeRepository: SwipeRepository,
    onNavigateToLikedRecipes: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val viewModel: SwipeViewModel = viewModel {
        SwipeViewModel(recipeRepository, swipeRepository)
    }

    val swipeSession by viewModel.swipeSession.collectAsState()
    val deckState = swipeSession.deckState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover Recipes") },
                actions = {
                    IconButton(onClick = onNavigateToLikedRecipes) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Text(swipeSession.history.likeCount.toString())
                        }
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Liked recipes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                deckState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                deckState.currentRecipe == null -> {
                    EmptyState(
                        onRefresh = { viewModel.loadRecipes() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            deckState.nextRecipes.take(2).forEachIndexed { index, recipe ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(500.dp)
                                        .offset(y = (8 * (index + 1)).dp)
                                        .padding(horizontal = (8 * (index + 1)).dp),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 4.dp
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                    }
                                }
                            }

                            SwipeableRecipeCard(
                                recipe = deckState.currentRecipe,
                                onSwipe = { swipeType ->
                                    viewModel.swipe(swipeType)
                                },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        SwipeActionButtons(
                            onDislike = { viewModel.swipe(SwipeType.DISLIKE) },
                            onSkip = { viewModel.swipe(SwipeType.SKIP) },
                            onLike = { viewModel.swipe(SwipeType.LIKE) },
                            onSuperLike = { viewModel.swipe(SwipeType.SUPER_LIKE) },
                            onUndo = { viewModel.undoSwipe() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        SwipeStats(swipeSession.history)
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeActionButtons(
    onDislike: () -> Unit,
    onSkip: () -> Unit,
    onLike: () -> Unit,
    onSuperLike: () -> Unit,
    onUndo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onUndo,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Undo",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        FilledIconButton(
            onClick = onDislike,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Red.copy(alpha = 0.1f),
                contentColor = Color.Red
            )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dislike",
                modifier = Modifier.size(28.dp)
            )
        }

        FilledIconButton(
            onClick = onSkip,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Gray.copy(alpha = 0.1f),
                contentColor = Color.Gray
            )
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Skip"
            )
        }

        FilledIconButton(
            onClick = onSuperLike,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Blue.copy(alpha = 0.1f),
                contentColor = Color.Blue
            )
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Super Like"
            )
        }

        FilledIconButton(
            onClick = onLike,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Green.copy(alpha = 0.1f),
                contentColor = Color.Green
            )
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Like",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun SwipeStats(history: com.example.cookingassistant.model.SwipeHistory) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Likes", history.likeCount, Color.Green)
        StatItem("Super", history.superLikeCount, Color.Blue)
        StatItem("Skips", history.skipCount, Color.Gray)
        StatItem("Nopes", history.dislikeCount, Color.Red)
    }
}

@Composable
fun StatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun EmptyState(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No more recipes!",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Try adjusting your preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRefresh) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh")
        }
    }
}