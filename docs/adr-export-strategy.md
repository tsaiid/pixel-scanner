# ADR: Export Strategy

## Status

Accepted.

## Context

Export must not always rebuild a PDF. The current code already distinguishes clean sessions, modified sessions, and JPEG export. The decision tree needs to be documented so future work does not collapse everything into one hardcoded path.

Supporting notes:

* [`docs/mlkit-result-persistence-fields.md`](/Users/tsaiid/git/pixel-scanner/docs/mlkit-result-persistence-fields.md)
* [`docs/navigation-graph.md`](/Users/tsaiid/git/pixel-scanner/docs/navigation-graph.md)

## Decision

Use an explicit export decision tree:

* If the request is PDF and the session is clean with an original PDF, reuse the original PDF path.
* If the request is PDF and the session is dirty, rebuild from the current page set.
* If the request is JPEG, export the active page set as images and preserve page-level results.
* Keep the decision logic keyed off session state, request format, and dirty flags.
* Keep export results explicit so the UI can handle success, partial success, failure, and cancellation.

## Consequences

* Clean sessions avoid unnecessary decode and rebuild work.
* Modified sessions still have a deterministic PDF path.
* JPEG export remains distinct from PDF export instead of being treated as a special case of PDF generation.
* The export path stays testable because the decision tree is separate from storage I/O.

## Deferred

* Incremental PDF reassembly is not a v1 requirement.
* Alternative PDF libraries or more advanced rewriter strategies remain optional future evaluation items.
* Export backgrounding is handled in a separate ADR and is not implied by this decision.
