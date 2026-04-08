# String Resource Policy

This note records the Phase 6 string-resource slice.

User-facing text now flows through `UiText` so ViewModel and domain code can keep message intent without hardcoding presentation strings. Static copy uses resource IDs, while data-derived messages still use `DynamicString` until a dedicated resource key exists.

Presentation layers resolve `UiText` at the boundary with `asString(context)`. That keeps composables, toasts, snackbars, and progress labels aligned on the same message model without leaking Android resource lookups into business logic.
