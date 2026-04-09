# Productization Alignment Notes

Resolved in the Phase 0 alignment slice:

* README now reflects the current `compileSdk` / `targetSdk` 35 build and lists completed, in-progress, and planned capabilities.
* `spec.md` now matches the current build target instead of implying an older API level.
* `todo.md` now matches the current `compileSdk` / `targetSdk` 35 build.
* `PRODUCTIZATION_TODO.md` marks the alignment sub-slice complete so the backlog reflects the current documentation state.
* AGP 9.0.0 now relies on the app module's `org.jetbrains.kotlin.plugin.compose` plugin only; the unused `org.jetbrains.kotlin.android` alias was removed from the root plugin block and version catalog.
* The app and benchmark modules now target Java / Kotlin JVM 17 instead of the legacy 1.8 bytecode level.
