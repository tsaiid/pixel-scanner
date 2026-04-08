# Review Effect Channel

This note records the Phase 7 effect-channel slice.

The review flow now uses `ReviewUiEffect` for one-off events, including navigation, undo, export success, and transient snackbar messages. Persistent `ReviewUiState` keeps only screen state that should survive recomposition and collection changes.

That split keeps transient feedback out of state while preserving the existing review screen architecture.
