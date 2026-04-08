package com.example.pixelscanner.app

enum class AppScreen {
    Home,
    Review;

    val route: String
        get() = name
}
