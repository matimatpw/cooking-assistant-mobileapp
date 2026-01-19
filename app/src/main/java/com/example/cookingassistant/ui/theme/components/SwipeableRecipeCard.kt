package com.example.cookingassistant.ui.theme.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cookingassistant.model.Recipe
import com.example.cookingassistant.model.SwipeType
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.background


/**
 * Swipeable recipe card component
 */
@Composable
fun SwipeableRecipeCard(
    recipe: Recipe,
    onSwipe: (SwipeType) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val rotation by remember { derivedStateOf { (offsetX / 20f).coerceIn(-15f, 15f) } }

    val scope = rememberCoroutineScope()
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }

    // Swipe threshold
    val swipeThreshold = 300f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(500.dp)
            .graphicsLayer {
                translationX = offsetX + animatedOffsetX.value
                translationY = offsetY + animatedOffsetY.value
                rotationZ = rotation
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            // Determine swipe type
                            val swipeType = when {
                                offsetX > swipeThreshold -> SwipeType.LIKE
                                offsetX < -swipeThreshold -> SwipeType.DISLIKE
                                offsetY < -swipeThreshold -> SwipeType.SUPER_LIKE
                                offsetY > swipeThreshold -> SwipeType.SKIP
                                else -> null
                            }

                            if (swipeType != null) {
                                // Animate card off screen
                                val targetX = when (swipeType) {
                                    SwipeType.LIKE -> 1000f
                                    SwipeType.DISLIKE -> -1000f
                                    else -> offsetX
                                }
                                val targetY = when (swipeType) {
                                    SwipeType.SUPER_LIKE -> -1000f
                                    SwipeType.SKIP -> 1000f
                                    else -> offsetY
                                }

                                animatedOffsetX.animateTo(
                                    targetX,
                                    animationSpec = tween(200)
                                )
                                animatedOffsetY.animateTo(
                                    targetY,
                                    animationSpec = tween(200)
                                )

                                onSwipe(swipeType)
                            } else {
                                // Snap back to center
                                animatedOffsetX.animateTo(0f, animationSpec = tween(200))
                                animatedOffsetY.animateTo(0f, animationSpec = tween(200))
                            }

                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Recipe image
            AsyncImage(
                model = recipe.mainPhotoUri,
                contentDescription = recipe.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.6f }
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black)
                        )
                    )
            )

            // Swipe indicators
            SwipeIndicators(offsetX, offsetY, swipeThreshold)

            // Recipe info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = recipe.name,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = recipe.description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip("${recipe.cookingTime} min")
                    InfoChip(recipe.difficulty.name)
                    InfoChip("${recipe.servings} servings")
                }
            }
        }
    }
}

@Composable
fun SwipeIndicators(offsetX: Float, offsetY: Float, threshold: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Like indicator (right)
        if (offsetX > 50) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(32.dp),
                color = Color.Green.copy(alpha = (offsetX / threshold).coerceIn(0f, 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "LIKE",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Dislike indicator (left)
        if (offsetX < -50) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(32.dp),
                color = Color.Red.copy(alpha = (abs(offsetX) / threshold).coerceIn(0f, 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "NOPE",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Super like indicator (up)
        if (offsetY < -50) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(32.dp),
                color = Color.Blue.copy(alpha = (abs(offsetY) / threshold).coerceIn(0f, 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SUPER LIKE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Skip indicator (down)
        if (offsetY > 50) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                color = Color.Gray.copy(alpha = (offsetY / threshold).coerceIn(0f, 0.8f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "SKIP",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
