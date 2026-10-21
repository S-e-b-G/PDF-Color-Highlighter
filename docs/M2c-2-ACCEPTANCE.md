# M2c.2 acceptance

## Baseline
M2c.1 accepted by user: build/Lint/install successful, 74 JVM tests with
zero failures/errors, clipping PASS and device acceptance.

## New source
50 recents, exact-URI deduplication, persistent page/full normalized viewport,
startup/Open lists, list removal, unavailable-state messaging, serialized
AtomicFile metadata storage, retained AndroidViewModel native session.

No dependency/engine/lock/wrapper/native proof/geometry/gesture changes.
No original PDF writes. No annotation/save/recovery completion claim.

## Tests
Expected JVM total: 94 = accepted 74 + 20 new tests.
Run clipping instrumentation and synthetic recent-storage instrumentation.
The author did not execute Kotlin, Gradle, JUnit, Lint, PowerShell or Android.
Expected counts are not passed-test reports.

## Device checks
1. Fresh launch shows Browse/recents, not an automatically opened PDF.
2. Open A, change page/zoom/X/Y, wait two seconds. Open B and do likewise.
3. Reopen A and B from recents. Full reading position, including Y, restores.
4. Previous/Next/jump preserve zoom and proportional X, but start at top.
5. Back out, relaunch and reopen a recent document. Position restores.
6. Rotate during reading, opening and page rendering. Session remains usable.
7. Recheck properties, page-jump draft rotation, Fit Page/Width, clipping,
   12 visible tools, pinch/pan and one-second rectangular feedback.
8. With a disposable PDF, remove/move it externally and try its recent entry.
   Failure must not crash. Browse another file to continue. Retirer deletes
   only the list entry, never the PDF.
9. Optional: wait two seconds, press Home, force-stop in Android Settings,
   relaunch and select a recent. Do not uninstall or clear app data.

## Persistence limits
Metadata is filesDir/reader-recents-v1.bin, with bounded strict UTF-8 decoding,
version header and SHA-256 footer. One worker serializes reads and writes.
AtomicFile stages the replacement; committed bytes are checked by read-back.

Use platform AtomicFile instead of adding the preferred DataStore dependency
for this small whole-list snapshot. Existing pinned dependencies remain intact.
This metadata is not annotation recovery, a PDF sidecar or an editable work copy.

Viewport changes debounce 750ms; continuous movement checkpoints approximately
every five seconds, subject to scheduling and disk I/O. Page/open/lifecycle
transitions enqueue immediately. Process death before write completion can
lose recent reading movement. No zero-loss or power-failure guarantee.

Read errors preserve the state file and disable writes for that ViewModel
session. Relaunch retries reading. Write errors retain memory and warn;
subsequent changes/lifecycle checkpoints can retry. No corrupt file is
automatically deleted or silently replaced with an empty list.

Persist only picker-granted read access. Moved/deleted files may need Browse
again. Exact URI defines identity; a changed URI is not guessed from a name.
Eviction/removal does not release potentially shared grants or recovery data.
Long-term grant cleanup remains a hardening item.

Reopening restores fit-relative zoom/normalized anchors, not identical pixels
across orientation changes. Page is clamped if external edits shorten the PDF.
External revision/conflict detection is not implemented in this read-only step.

ViewModel retention is not process persistence. Saved-activity recreation can
reopen its saved URI; ordinary new launch only shows recents. Existing source
copies remain read-only cache files, not durable editable storage. Zoom still
magnifies the existing 1600px render.

## Next
After acceptance: durable working-document/save foundation, then sharper
rendering as separate bounded work. Production selection, handles, notes,
history, saving, autosave/recovery and settings remain pending. M2 is not complete.

## Public API evidence
https://developer.android.com/reference/android/util/AtomicFile
https://developer.android.com/topic/libraries/architecture/viewmodel
https://developer.android.com/reference/androidx/lifecycle/ViewModelProvider
https://developer.android.com/reference/kotlin/androidx/compose/ui/viewinterop/package-summary
https://developer.android.com/training/data-storage/shared/documents-files
