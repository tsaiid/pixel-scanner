# Dynamic Color Policy

Pixel Scanner v1 uses a brand-first color policy.

Current product flows follow the brand palette:

- Home
- Scanner
- Review
- Export complete

These flows do not use Android dynamic color, even on supported devices. The app keeps one visual identity across light and dark mode by using the centralized brand theme tokens.

Dynamic color is reserved for future system-owned or non-product utility surfaces if the app ever introduces them. That decision keeps the current scanning flow stable and avoids mixing the brand plum palette with per-device system colors.
