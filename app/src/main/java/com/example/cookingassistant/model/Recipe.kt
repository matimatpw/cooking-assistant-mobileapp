package com.example.cookingassistant.model

/**
 * Data model representing a recipe
 * @param id Unique identifier for the recipe
 * @param name Name of the dish
 * @param ingredients List of ingredients needed
 * @param instructions Step-by-step cooking instructions
 * @param cookingTime Estimated cooking time in minutes
 */
data class Recipe(
    val id: Int,
    val name: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val cookingTime: Int
)
