# Dependency Injection Decision

Current milestone decision: keep manual constructor injection.

## Decision

- Use `AppContainer` as the app-level composition root.
- Build `HomeViewModelFactory` and `ReviewViewModelFactory` from that container.
- Keep dependencies explicit through constructors and factory creation.

## Why

- The current graph is still small enough that manual wiring stays readable.
- The app already gets testability benefits from constructor injection without adding a DI framework.
- Deferring Hilt avoids introducing migration work before there is a clear scaling trigger.

## Deferred

- Hilt migration is intentionally deferred for this milestone.
- Revisit Hilt only when an explicit trigger appears, such as:
  - repeated wiring across multiple feature graphs
  - scoping or assisted-injection needs that manual wiring does not handle cleanly
  - test setup or container maintenance becoming materially harder

## Current Code Path

- `app/src/main/java/com/example/pixelscanner/app/AppContainer.kt`
- `app/src/main/java/com/example/pixelscanner/feature/home/HomeViewModel.kt`
- `app/src/main/java/com/example/pixelscanner/feature/review/ReviewViewModel.kt`
