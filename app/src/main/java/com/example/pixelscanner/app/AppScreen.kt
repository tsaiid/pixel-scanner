package com.example.pixelscanner.app

sealed interface AppScreen {
    data object Home : AppScreen
    data object Review : AppScreen
}
