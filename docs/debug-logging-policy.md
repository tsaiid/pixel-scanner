# Debug Logging Policy

This project keeps observability lightweight and debug-only.

## Tags

- `PixelScannerPerf` is the shared tag for timing and structured lifecycle events.
- `ImageEnhancerMem` remains the dedicated tag for memory snapshots in the preview enhancement pipeline.

## What We Log

- Timing for scanner return handling, session ingestion, preview enhancement, and export.
- High-level lifecycle events such as cache hit/miss, outcome, format, and page counts.
- Exception class names for debug-only failures, without raw file paths or URI strings.

## What We Do Not Log

- Raw source URIs, filesystem paths, or document contents.
- User data that would not be needed to diagnose a timing or control-flow issue.

## Release Behavior

- The shared logger is silent unless the `PixelScannerPerf` tag is enabled via `logcat`.
- Performance logs are intended for local diagnosis and benchmark validation, not release analytics.
