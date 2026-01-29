# Google ML Kit Document Scanner Limitations & UX Decisions

## 1. Scanner Session Continuity
**Limitation:**
The Google ML Kit Document Scanner (`GmsDocumentScanner`) operates as a self-contained Activity. Once the user taps "Done" and the result (`GmsDocumentScanningResult`) is returned to our app, the scanner session is effectively **terminated** and its resources are released.

**Implication:**
It is **technically impossible** to "go back" to the previous ML Kit scanner session to continue editing (cropping/rotating/filtering) the *exact same* set of images that were just returned.

## 2. Navigation Strategy ("Back" vs. "Rescan")
**Decision:**
To handle the user's desire to "go back" from the Review Screen:
- We cannot restore the previous state inside the ML Kit UI.
- Therefore, the "Back" action on the Review Screen is implemented as **"Discard current results and start a new Scan"**.
- This automatically re-launches the scanner, giving the user the feeling of returning to the scanning context, even though it is a fresh session.

**UX Update:**
To avoid confusion, the button labeled "Back" on the Review Screen should be renamed to **"Discard & Rescan"** (or similar) to clearly indicate that the current review session will be discarded.
