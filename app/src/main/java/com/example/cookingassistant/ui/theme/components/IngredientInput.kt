package com.example.cookingassistant.ui.theme.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cookingassistant.R
import com.example.cookingassistant.model.Ingredient

/**
 * Dynamic ingredient input list component
 * Allows adding, editing, and removing ingredients
 */
@Composable
fun IngredientInputList(
    ingredients: List<Ingredient>,
    onIngredientsChange: (List<Ingredient>) -> Unit,
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
                text = stringResource(R.string.ingredients),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedButton(
                onClick = {
                    onIngredientsChange(ingredients + Ingredient("", "", null))
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.add))
            }
        }

        // List of ingredient inputs
        if (ingredients.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.no_ingredients_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            ingredients.forEachIndexed { index, ingredient ->
                IngredientInputItem(
                    ingredient = ingredient,
                    index = index,
                    onIngredientChange = { updatedIngredient ->
                        val updatedList = ingredients.toMutableList()
                        updatedList[index] = updatedIngredient
                        onIngredientsChange(updatedList)
                    },
                    onRemove = {
                        // Only allow removal if there's more than 1 ingredient
                        if (ingredients.size > 1) {
                            onIngredientsChange(ingredients.filterIndexed { i, _ -> i != index })
                        }
                    },
                    canRemove = ingredients.size > 1
                )
            }
        }
    }
}

/**
 * Individual ingredient input item
 */
@Composable
fun IngredientInputItem(
    ingredient: Ingredient,
    index: Int,
    onIngredientChange: (Ingredient) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header with index and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ingredient_number, index + 1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onRemove,
                    enabled = canRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.remove_ingredient),
                        tint = if (canRemove) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name field (required)
            OutlinedTextField(
                value = ingredient.name,
                onValueChange = { onIngredientChange(ingredient.copy(name = it)) },
                label = { Text(stringResource(R.string.name_field)) },
                placeholder = { Text(stringResource(R.string.name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity field (required)
            OutlinedTextField(
                value = ingredient.quantity,
                onValueChange = { onIngredientChange(ingredient.copy(quantity = it)) },
                label = { Text(stringResource(R.string.quantity_field)) },
                placeholder = { Text(stringResource(R.string.quantity_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Notes field (optional)
            OutlinedTextField(
                value = ingredient.notes ?: "",
                onValueChange = {
                    onIngredientChange(ingredient.copy(notes = if (it.isBlank()) null else it))
                },
                label = { Text(stringResource(R.string.notes_optional)) },
                placeholder = { Text(stringResource(R.string.notes_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
