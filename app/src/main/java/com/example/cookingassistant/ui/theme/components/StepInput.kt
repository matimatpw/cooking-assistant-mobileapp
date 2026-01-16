package com.example.cookingassistant.ui.theme.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cookingassistant.R
import com.example.cookingassistant.model.MediaType
import com.example.cookingassistant.model.RecipeStep
import com.example.cookingassistant.model.StepMedia

/**
 * Dynamic recipe step input list component
 * Allows adding, editing, and removing steps
 */
@Composable
fun StepInputList(
    steps: List<RecipeStep>,
    onStepsChange: (List<RecipeStep>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with title and add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.instructions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = {
                    val newStepNumber = steps.size + 1
                    onStepsChange(
                        steps + RecipeStep(
                            stepNumber = newStepNumber,
                            instruction = "",
                            durationMinutes = null,
                            mediaItems = emptyList(),
                            tips = null
                        )
                    )
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.add_step))
            }
        }

        // List of step inputs
        if (steps.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.no_steps_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            steps.forEachIndexed { index, step ->
                StepInputItem(
                    step = step,
                    index = index,
                    onStepChange = { updatedStep ->
                        val updatedList = steps.toMutableList()
                        updatedList[index] = updatedStep.copy(stepNumber = index + 1)
                        onStepsChange(updatedList)
                    },
                    onRemove = {
                        // Only allow removal if there's more than 1 step
                        if (steps.size > 1) {
                            val updatedList = steps.filterIndexed { i, _ -> i != index }
                            // Renumber steps after removal
                            onStepsChange(
                                updatedList.mapIndexed { i, s -> s.copy(stepNumber = i + 1) }
                            )
                        }
                    },
                    canRemove = steps.size > 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < steps.size - 1,
                    onMoveUp = {
                        if (index > 0) {
                            val updatedList = steps.toMutableList()
                            val temp = updatedList[index]
                            updatedList[index] = updatedList[index - 1]
                            updatedList[index - 1] = temp
                            // Renumber steps
                            onStepsChange(
                                updatedList.mapIndexed { i, s -> s.copy(stepNumber = i + 1) }
                            )
                        }
                    },
                    onMoveDown = {
                        if (index < steps.size - 1) {
                            val updatedList = steps.toMutableList()
                            val temp = updatedList[index]
                            updatedList[index] = updatedList[index + 1]
                            updatedList[index + 1] = temp
                            // Renumber steps
                            onStepsChange(
                                updatedList.mapIndexed { i, s -> s.copy(stepNumber = i + 1) }
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * Individual step input item
 */
@Composable
fun StepInputItem(
    step: RecipeStep,
    index: Int,
    onStepChange: (RecipeStep) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean = true,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header with step number, move buttons, and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.step_number, index + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    // Move up button
                    TextButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("↑", style = MaterialTheme.typography.titleSmall)
                    }

                    // Move down button
                    TextButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("↓", style = MaterialTheme.typography.titleSmall)
                    }
                }

                IconButton(
                    onClick = onRemove,
                    enabled = canRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.remove_step),
                        tint = if (canRemove) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instruction field (required)
            OutlinedTextField(
                value = step.instruction,
                onValueChange = { onStepChange(step.copy(instruction = it)) },
                label = { Text(stringResource(R.string.instruction_field)) },
                placeholder = { Text(stringResource(R.string.instruction_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Duration and tips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Duration field (optional)
                OutlinedTextField(
                    value = step.durationMinutes?.toString() ?: "",
                    onValueChange = {
                        val duration = it.toIntOrNull()
                        onStepChange(step.copy(durationMinutes = duration))
                    },
                    label = { Text(stringResource(R.string.duration_field)) },
                    placeholder = { Text(stringResource(R.string.duration_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tips field (optional)
            OutlinedTextField(
                value = step.tips ?: "",
                onValueChange = {
                    onStepChange(step.copy(tips = if (it.isBlank()) null else it))
                },
                label = { Text(stringResource(R.string.tips_optional)) },
                placeholder = { Text(stringResource(R.string.tips_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(8.dp)
            )

            // Media items section
            Spacer(modifier = Modifier.height(8.dp))

            StepMediaPicker(
                mediaItems = step.mediaItems,
                onMediaItemsChange = { newMediaItems ->
                    onStepChange(step.copy(mediaItems = newMediaItems))
                }
            )
        }
    }
}

/**
 * Media picker for recipe step
 * Allows adding multiple photos/videos to a step
 */
@Composable
fun StepMediaPicker(
    mediaItems: List<StepMedia>,
    onMediaItemsChange: (List<StepMedia>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.step_media),
            style = MaterialTheme.typography.labelMedium
        )

        // Display existing media items
        if (mediaItems.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mediaItems.forEachIndexed { index, mediaItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Media preview
                            AsyncImage(
                                model = mediaItem.uri,
                                contentDescription = mediaItem.caption,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Fit
                            )

                            // Media type and caption
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = when (mediaItem.type) {
                                        MediaType.PHOTO -> stringResource(R.string.photo)
                                        MediaType.VIDEO -> stringResource(R.string.video)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (mediaItem.caption != null) {
                                    Text(
                                        text = mediaItem.caption,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            // Remove button
                            IconButton(
                                onClick = {
                                    val updatedList = mediaItems.filterIndexed { i, _ -> i != index }
                                    onMediaItemsChange(updatedList)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.remove_media),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add media button
        MediaPickerButton(
            label = if (mediaItems.isEmpty())
                stringResource(R.string.add_step_photo)
            else
                stringResource(R.string.add_another_photo),
            currentMediaUri = null,
            onMediaSelected = { uri ->
                uri?.let {
                    val newMediaItem = StepMedia(
                        type = MediaType.PHOTO,
                        uri = it.toString(),
                        caption = null,
                        thumbnailUri = null
                    )
                    onMediaItemsChange(mediaItems + newMediaItem)
                }
            }
        )
    }
}
