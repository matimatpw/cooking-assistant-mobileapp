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
                Text("Add")
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
                    text = "No ingredients added yet. Click \"Add\" to start.",
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
                        onIngredientsChange(ingredients.filterIndexed { i, _ -> i != index })
                    }
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
                    text = "Ingredient ${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove ingredient",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name field (required)
            OutlinedTextField(
                value = ingredient.name,
                onValueChange = { onIngredientChange(ingredient.copy(name = it)) },
                label = { Text("Name *") },
                placeholder = { Text("e.g., Tomatoes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity field (required)
            OutlinedTextField(
                value = ingredient.quantity,
                onValueChange = { onIngredientChange(ingredient.copy(quantity = it)) },
                label = { Text("Quantity *") },
                placeholder = { Text("e.g., 2 cups or 500g") },
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
                label = { Text("Notes (optional)") },
                placeholder = { Text("e.g., diced, room temperature") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
