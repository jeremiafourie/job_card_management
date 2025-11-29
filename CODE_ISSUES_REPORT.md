# Codebase Issues Report

## Blocking / Functional
- **Gradle wrapper lock prevents local builds**: `~/.gradle/wrapper/dists/gradle-8.13-bin/.../gradle-8.13-bin.zip.lck` causes build failures until manually removed; add a cleanup step or adjust wrapper permissions.
- **Fixed asset checkout depends solely on local status history**: No server reconciliation or conflict handling; if history desynchronises, checkout/return can fail or allow duplicates. Needs backend source-of-truth or sync/conflict resolution.
- **No job creation flow**: `JobsScreen` exposes `onCreateJob` but MainActivity leaves a TODO; the “+” button does nothing. Implement job creation/navigation.

## UX / Behavior
- **Checkout feedback is slow**: UI waits for DB write; snackbar added, but no optimistic state. Consider optimistic update or loading state in lists.
- **Deprecated icons/components**: `ArrowBack`, `Divider` warnings; update to AutoMirrored/HorizontalDivider variants.
- **Unused variables and preview APIs**: Some viewmodels (JobsViewModel savedStateHandle) and Compose previews show warnings; clean up or opt-in annotations.

## Data & Persistence
- **Destructive migrations**: `fallbackToDestructiveMigration` wipes data on version bumps; real migrations and `exportSchema` are needed before production.
- **Media stored as JSON/URIs**: Photos and receipts are stored in JSON strings/rows without checksum/size; consider normalised media tables and relative paths for portability.
- **No JDK/compile target update**: Build uses source/target 8 with JDK 21; update Android compileOptions or suppress deprecation as needed.

## Documentation Gaps
- Job creation flow and fixed-asset checkout UX are not documented; add to SYSTEM_DESIGN.md once implemented.
- Sync strategy for status histories (jobs/fixed) is undefined; document expected server reconciliation to avoid local-only divergence.

## Testing / Reliability
- No automated tests around checkout/return, autosave debounce, or purchases; add unit/UI tests for these flows.
- Autosave (JobDetail) has no error handling; failures are silent. Add UI feedback on save failures.
