# Codebase Issues Report

## Blocking / Functional
- **Gradle wrapper location is read-only**: `~/.gradle/wrapper/dists/gradle-8.13-bin/.../gradle-8.13-bin.zip.lck` cannot be removed in this environment and DNS is blocked for `services.gradle.org`, so the wrapper cannot download Gradle. Use a writable `GRADLE_USER_HOME` and a pre-downloaded 8.13 distribution (or delete the lock on the host) to build.
- **Fixed asset checkout depends solely on local status history**: No server reconciliation or conflict handling; if history desynchronises, checkout/return can fail or allow duplicates. Needs backend source-of-truth or sync/conflict resolution.

## UX / Behavior
- **Checkout feedback is slow**: UI waits for DB write; snackbar added, but no optimistic state. Consider optimistic update or loading state in lists.
- **Preview/unused warnings**: Compose preview-only variables and unused parameters should be annotated with `@PreviewParameter`/`@Suppress("Unused")` or removed to keep builds warning-free.

## Data & Persistence
- **Destructive migrations**: `fallbackToDestructiveMigration` wipes data on version bumps; real migrations and `exportSchema` are needed before production.
- **JDK/compile target alignment**: Updated to Java 17, but ensure CI/toolchains are pinned to the same version to avoid surprises.
- **Photos still serialized as JSON**: They now point to app-managed FileProvider URIs (copied into app storage), so expiry risk is low. A future migration to a normalized `job_photos` table would simplify querying/sync and remove JSON parsing.

## Documentation Gaps
- Sync strategy for status histories (jobs/fixed) is undefined; document expected server reconciliation to avoid local-only divergence.

## Testing / Reliability
- No automated tests around checkout/return, autosave debounce, or purchases; add unit/UI tests for these flows.
- Autosave (JobDetail) has no error handling; failures are silent. Add UI feedback on save failures.
