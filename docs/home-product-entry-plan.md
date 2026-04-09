# Home Product Entry Plan

This note closes the Phase 6 home-surface planning slice. It stays at the product-entry level and does not redefine the full home implementation yet.

## Current Start Flow

- The app currently has a single start surface with a primary scan button.
- If a recoverable unfinished session exists, the app shows a modal prompt to restore or discard it.
- There is no recent-scans surface today, and v1 should not introduce one as part of this refactor slice.

## V1 Product Jobs

The home screen should do three things well:

1. Start a new scan quickly.
2. Help the user continue an unfinished session when one exists.
3. Explain the app’s current state without making the entry flow feel like a dead-end.

## Explicit V1 Decisions

### Recent Scans

Recent scans do not appear in v1.

- Reason: history is a separate product surface and is not required to support the core scan-review-export loop.
- Keeping it out avoids turning the home screen into a storage browser before the review flow is fully productized.

### Resume Session

Resume session does appear in v1, but only for a recoverable unfinished session.

- This is not a generic “resume any prior scan” history feature.
- It remains a context-aware recovery prompt tied to the current unfinished session behavior.
- The user can restore or discard that session before starting a new scan.

## Wireframe Plan

### Structure

- Top area: brand mark, app name, and a short scanner-oriented tagline.
- Primary area: one dominant `Start Scanning` button.
- Recovery area: conditional card for an unfinished session, shown only when recovery is available.
- Supporting copy: short helper text that explains the app is centered on scanning, review, and export.

### Layout Behavior

- The primary action must remain visually dominant on small screens.
- The recovery card should sit above or near the primary action, but not compete with it.
- If there is no recoverable session, the screen should stay simple rather than filling space with placeholder content.

### Wireframe

```text
Home
├─ Brand / Logo
├─ App Name
├─ Short tagline
├─ Primary action: Start Scanning
├─ Optional recovery card
│  ├─ Unfinished session title
│  ├─ Page count / missing asset note
│  ├─ Restore
│  └─ Discard
└─ Minimal helper text
```

## Product Boundary

- v1 home is an entry point, not a dashboard.
- v1 uses the recoverable-session prompt as the only secondary home behavior.
- Recent scans and richer history belong to a later product surface, after the scan-review-export loop is stable.
