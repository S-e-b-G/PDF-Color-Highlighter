# M3k — automatic commit and app-managed highlight editing

Baseline accepted by user: M3j,534 JVM tests,18 native suites, build/Lint/install and manual
Save As checks. Expected560 JVM tests (26 new),19 native suites (one new interaction proof).

## Interaction
Choose a highlight color and drag text. A valid final release commits a native highlight
locally without Valider. A final moved handle commits updated boundaries to the SAME ID.
No commits for MOVE feedback, canceled/stale gestures, pinch or unchanged handle taps.
Tap an app-created highlight (including after reopening its source or an app-exported PDF):
restore selection handles and its checked palette slot. Different slot recolors in place;
checked slot deletes. Tap Hand to deselect without deleting. Starting a new text range
creates a new highlight rather than changing the previous one's boundaries. Operations
serialize and temporarily block input while the durable checkpoint/render completes.
Failed creation retains a retry button and local error; a new gesture can try again.
No completion/speed guarantee beyond measured device results.

## Scope and safeguards
This increment edits recognized app-created native Highlights, including M3i/M3j existing
ones and app-created annotations kept in exported PDFs. Arbitrary third-party highlights,
missing/changed app metadata, non-palette colors, hidden/locked/read-only annotations and
unresolvable/disjoint ranges are NOT editable through this UI yet. Those annotations remain
untouched. This is an explicit bounded step, not universal imported-annotation editing.
Overlapping managed highlights refuse ambiguous taps; touch an exclusive portion instead.
Resolved handles require full matching native geometry (0.03 point serialization tolerance),
not guessed character offsets. Existing selection/accent/flow algorithms remain unchanged.
Recolor/resize updates the existing annotation in place, not delete/recreate. Automatic
Contents follows changed quotation. Explicit manual mode or externally changed Contents
never gets replaced with an extracted quotation merely because boundaries moved.
No manual Contents editor is introduced here.

Undo/Redo restores complete checksummed PDF checkpoints, preserving full original annotation
dictionaries, additional metadata and unrelated annotations even when undoing deletion.
History is session-only, bounded to16 commands/64MiB unique snapshots, always retaining the
last command even if one large PDF exceeds the budget. Required immutable snapshots are
pinned against pruning. SHA-256 is rechecked before restoring a historical checkpoint.
A new edit after Undo invalidates Redo. Recovery after reopen keeps native edits, not undo
history. Export remains M3j Save As; original overwrite/Save remains unavailable. Previously
exported copies are immutable relative to later local editing/undo. No original URI writes.

## Device acceptance
1. In TestFile.pdf choose a color and drag text; release. No Valider should be required.
   Tap Hand, change pages and return: the highlight is real and remains present.
2. Tap its text, including in Hand mode. Both handles and its checked color must appear.
3. Tap another color: SAME annotation changes color, quotation/range stay unchanged.
4. Tap its checked color: annotation disappears. Undo restores it and Redo deletes it.
5. Undo deletion, tap the highlight, drag EACH handle and release. Only that highlight changes;
   verify Citation and no overlapping duplicate by exporting and inspecting in another reader.
6. Undo/Redo resize and recolor. After Undo, make a new highlight; Redo must be unavailable.
7. Reopen source and an exported copy. App-created highlights must still be tappable/editable.
8. Test reverse selection, accents, lists, zoom/Fit/rotation, handle tap without movement,
   pinch during an unfinished gesture and multi-touch cancellation: no spurious annotation.
9. Export after editing. Compare colors, count, quotation in another reader; original and
   an earlier exported PDF must remain unchanged. Repeat Obsidian/Docear extraction.
10. Rapidly create several highlights; wait for the local operation to finish between gestures.
    Report measured pauses or dropped gestures rather than assuming background performance.
11. Overlapping highlights: an ambiguous tap must not choose/delete an arbitrary annotation.

## Verification boundaries
Author environment has no Java/Kotlin/Gradle/PowerShell/Android runtimes. Source patch replay,
XML/resources, baseline test preservation, declaration counts, native call-site review,
independent gesture/history models and ZIP integrity are checked here, NOT JUnit/Compose/JNI.
The new19th suite runs on a synthetic self-authored private PDF: add, list, handle resolution,
recolor, resize, manual Contents preservation, delete, byte-exact undo/redo, pin maintenance,
reopen and branch history. Actual touch routing/automatic VM commits remain manual device
acceptance; the native suite is not a UI automation claim. Prior18 suites must also pass.

## M3k native fixture correction — 2026-11-01
User run: build/Lint and560 JVM tests passed, installation succeeded and prior18 native
suites passed. The19th stopped at "Native quads did not restore handles".
The new interaction suite's edit() helper incorrectly used Page.search geometry, unlike
production HighlightEdit.from(SelectionSpan), whose quads come from selected glyphs.
The correction changes ONLY this helper to MuPdfTextSnapshot.extract + PageTextSelector.select
+ HighlightEdit.from, asserts the expected quotation/multiple quads and exact pre-save restored
endpoints. The original POST-SAVE native endpoint restoration assertion remains, as do all
recolor/resize/delete/manual-text/stale-state/history assertions. No assertion is skipped.
Application version/code and560 JVM declarations remain unchanged. Rebuild/retest using
Validate-M3k.ps1; DeviceOnly is not sufficient for a changed native test/APK.
Author checked anchors/reference geometry/source integrity, not native execution. A successful
post-fix device run remains required; do not claim the app's touch workflow is accepted yet.
