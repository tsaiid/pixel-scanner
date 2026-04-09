# Static Analysis Toolchain

## Selected Tooling

- `Android lint` remains the existing platform-level check.
- `detekt` is the repo-wide Kotlin static-analysis lane.

## Why detekt

- It is Kotlin-aware and works well on Android source sets.
- It supports baselines, which lets the repo keep a strict "new issues only" policy without blocking on historical debt.
- It adds value beyond formatting-only tools by checking complexity and code-smell rules.

## Current Setup

- `app` and `benchmark` both apply the `detekt` Gradle plugin.
- Both modules share the same config file at [`config/detekt/detekt.yml`](/Users/tsaiid/git/pixel-scanner/config/detekt/detekt.yml).
- `app` keeps its current findings in [`app/detekt-baseline.xml`](/Users/tsaiid/git/pixel-scanner/app/detekt-baseline.xml).
- `benchmark` has no current findings and does not need a baseline file yet.
- CI runs `:app:detekt` and `:benchmark:detekt` alongside the existing debug build and lint checks.

## Maintenance Rule

- When detekt reports a new finding, fix the code first.
- If the finding is an accepted false positive or existing debt, add it to the appropriate baseline only after reviewing it.
