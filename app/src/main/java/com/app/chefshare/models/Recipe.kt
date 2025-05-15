package com.app.chefshare.models

import java.io.Serializable

data class Recipe(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val portion: String = "",
    val cookingTime: String = "",
    val ingredients: List<String> = listOf(),
    val steps: List<String> = listOf(),
    val mainImage: String = "",
    val stepImages: List<String> = listOf(),
    val userId: String = ""
) : Serializable