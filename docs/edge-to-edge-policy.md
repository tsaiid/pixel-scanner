# Edge-to-Edge Policy

This note records the Phase 6 edge-to-edge slice.

Pixel Scanner now uses a single window policy for the current product surfaces:

- The activity opts into edge-to-edge drawing.
- System bars are transparent.
- Status bar and navigation bar icon contrast is derived from the active surface color scheme.
- Home and the scanner resume screen use safe-drawing insets through their root scaffold.
- Review uses safe-drawing insets through its root scaffold.

This keeps the current surfaces intentional instead of depending on default system window fitting. The app still draws brand-led content behind the bars, but the interactive regions stay inside safe drawing bounds.
