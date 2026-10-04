package com.example.ui.navigation

enum class MainTab(val title: String) {
    IMAGE("Image"),
    VIDEO("Vidéo"),
    FILM("Film")
}

sealed class Screen {
    object Main : Screen()
    object Gallery : Screen()
    object Settings : Screen()
}
