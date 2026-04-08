# Feedback Pattern

This note records the Phase 7 feedback-pattern slice.

Current app feedback is split by intent:

- Snackbar: transient app-wide outcomes that do not require an immediate decision, including recoverable-session restore warnings, export completion notices, scanner launch failures, and open/share failures.
- Dialog: explicit user choices that block forward progress, including recover/discard confirmations and the export post-action chooser.
- Inline status: ongoing review state such as enhancement failures, export progress, and the scanner resume/loading state.

The implementation keeps those boundaries at the app shell and screen level instead of spreading ad hoc toasts through the codebase.
