# Productization Roadmap And Branching

This document defines the working rules for the productization refactor backlog in `PRODUCTIZATION_TODO.md`.

## Branch Naming Rules

Use short, purpose-specific branch names:

* `refactor/<milestone>-<slice>`
* `docs/<topic>` for documentation-only work
* `spike/<topic>` for disposable investigation branches

Rules:

* Keep the branch name tied to one milestone and one slice.
* Prefer lowercase words separated by hyphens.
* Use `refactor/` for production code or doc changes that directly support the refactor plan.
* Use `spike/` only for time-boxed investigation work that is expected to be discarded or summarized.
* Avoid packing multiple milestones into one branch.

Examples:

* `refactor/architecture-session-boundary`
* `refactor/session-lifecycle-ingestion`
* `refactor/performance-preview-benchmark`
* `docs/release-checklist`

## Milestones

The roadmap uses the milestone names already called out in `PRODUCTIZATION_TODO.md`.

### `architecture`

Expected outputs:

* Feature/package boundaries documented and reflected in code structure.
* Session ownership and repository boundaries defined.
* Navigation and state ownership decisions written down.

Completion criteria:

* Review and export responsibilities are separated from app shell concerns.
* Core session and repository boundaries are stable enough that later milestones do not need to redefine them.
* The architecture-related Phase 1 and Phase 2 items are either complete or explicitly deferred with a reason.

### `session-lifecycle`

Expected outputs:

* Session ingestion, metadata, cleanup, and recovery rules are documented.
* Recovery and discard/rescan behavior is unambiguous.
* Any temporary-file cleanup rules are written down.

Completion criteria:

* Session-owned files are the default path after scan completion.
* Unfinished sessions can be recovered or discarded without ambiguity.
* Cleanup behavior is documented well enough to support review and export flows without ad hoc rules.

### `performance`

Expected outputs:

* Baseline measurement doc or report.
* Sample set definitions for 1-page, multi-page, large-image, and long-document scenarios.
* Any chosen optimization strategy is justified by measurement, not guesswork.

Completion criteria:

* Baseline numbers exist for the core scenarios listed in `PRODUCTIZATION_TODO.md`.
* Benchmark or measurement evidence is available before adding higher-cost optimizations.
* Memory and export behavior are known for the target document sizes.

### `ux`

Expected outputs:

* Review, start, and export UX decisions captured in docs or implemented screens.
* String / label decisions for major user actions.
* Layout, navigation, and feedback patterns are consistent.

Completion criteria:

* The main scan-review-export loop is understandable without hidden state.
* Destructive actions, continuation actions, and post-export actions are clearly differentiated.
* Review and start flows have no unresolved major UX conflicts.

### `quality`

Expected outputs:

* Test strategy for ViewModel, use case, repository, and UI layers.
* Known failure cases and fallback behavior documented.
* Debug / diagnostic conventions defined.

Completion criteria:

* The project has a documented test plan for the main refactor surfaces.
* Quality risks are listed with an owner and next action.
* Release blockers are visible before release-prep begins.

### `release-prep`

Expected outputs:

* Release checklist.
* Documentation delta list.
* Final verification items for build, QA, and store readiness.

Completion criteria:

* The release checklist can be run without reopening architecture questions.
* Remaining known issues are explicitly triaged.
* The project is in a state where a release candidate can be prepared without speculative changes.

## Board Workflow

Use a small, board-column style flow:

* `Backlog` - identified but not yet scheduled.
* `Ready` - scoped, documented, and safe to start.
* `In Progress` - actively being implemented on a single branch.
* `Review` - ready for verification, doc updates, or merge.
* `Done` - merged or accepted and reflected back into `PRODUCTIZATION_TODO.md`.
* `Blocked` - waiting on a dependency, benchmark, or product decision.

Working rules:

* Each branch should map to one milestone slice.
* A slice moves to `Review` only after the doc or implementation output is complete enough to evaluate.
* A slice moves to `Done` only when the backlog entry is updated to match the current state.
* If a slice is blocked, record the reason and the next decision needed before starting a different milestone.

## Suggested Use

For this repo, the preferred execution order is:

1. `architecture`
2. `session-lifecycle`
3. `performance`
4. `ux`
5. `quality`
6. `release-prep`

That order matches the structure in `PRODUCTIZATION_TODO.md` and keeps structural decisions ahead of UX, quality, and release work.
