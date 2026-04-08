package com.example.pixelscanner.app

enum class AppScreen {
    Home,
    Scanner,
    Review,
    ExportComplete;

    val route: String
        get() = name
}
