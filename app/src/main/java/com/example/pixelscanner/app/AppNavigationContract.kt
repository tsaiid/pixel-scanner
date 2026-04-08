package com.example.pixelscanner.app

/**
 * Navigation contract for the productized scan flow.
 *
 * Scanner is still hosted as an embedded flow under `Home` at runtime, while
 * `ExportComplete` is a contract-level terminal state for the successful export
 * path. Both are part of the graph definition so the intended flow is explicit.
 */
object AppNavigationContract {
    val homeRoute: String = AppScreen.Home.route
    val scannerRoute: String = AppScreen.Scanner.route
    val reviewRoute: String = AppScreen.Review.route
    val exportCompleteRoute: String = AppScreen.ExportComplete.route

    val transitions: List<NavigationTransition> = listOf(
        NavigationTransition(
            from = homeRoute,
            to = scannerRoute,
            note = "Home launches the scanner flow.",
        ),
        NavigationTransition(
            from = scannerRoute,
            to = reviewRoute,
            note = "Scanner returns the scan result to review.",
        ),
        NavigationTransition(
            from = reviewRoute,
            to = exportCompleteRoute,
            note = "Successful export completes the flow.",
        ),
    )
}

data class NavigationTransition(
    val from: String,
    val to: String,
    val note: String,
)
