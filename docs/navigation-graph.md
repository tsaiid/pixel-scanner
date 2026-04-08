# Navigation Graph Contract

The current app navigation contract is intentionally explicit about the intended
product flow, even when some steps are still embedded rather than dedicated
destinations.

## Nodes

- `Home`: entry point that can launch scanning.
- `Scanner`: embedded scan flow owned by the home destination at runtime.
- `Review`: review and edit destination for scanned pages.
- `ExportComplete`: contract-level terminal state for a successful export.

## Transitions

- `Home -> Scanner`: user starts scanning from the home entry point.
- `Scanner -> Review`: scanner returns a scan result to the review flow.
- `Review -> ExportComplete`: successful export completes the flow.

## Runtime Notes

- The app currently uses a `NavHost` for `Home` and `Review`.
- `Scanner` remains an embedded flow inside `Home`.
- `ExportComplete` is defined as a contract-level terminal state and does not
  yet have a dedicated screen.
