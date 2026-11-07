# M3l — continuous native appearance, citation editing, timed edit feedback

Baseline: user560 JVM tests/build/Lint and19 native suites passed after M3k fixture correction.
Manual create/select/recolor/resize/delete/Undo/Redo/export/pinch/accents/rotation reported OK.
Reopen check was "IJ": not recorded as a pass. Appearance defects and ~1s deletion remain open.
Expected590 JVM tests (30 new),20 native suites (one new appearance/persistence proof).

## Appearance architecture
Standard PDF QuadPoints now use explicit horizontal row appearance bands for NEW or explicitly
updated app-managed highlights. This is NOT copying UI PreviewPaintBand rectangles into PDF.
A distinct NativeHighlightAppearance policy derives bounded bands from selected source glyphs,
filling selected word gaps without joining rows or arbitrary column gutters. Selected small/
zero-height ellipses may borrow only nearby selected same-row character height. Non-horizontal
quads keep their native geometry. Separate bounded SGCHGlyphQuads metadata retains exact
nondegenerate source glyph quads for existing hit/handle resolution; SGCHAppearanceQuads records
the intended standard quads. Actual native appearance is checked against that record before
allowing edits. Native selection/endpoints/quotation code is unchanged. Metadata decode is
bounded/versioned; unknown/malformed state refuses editing instead of guessing.
A completed selected native highlight no longer has a duplicate preview fill. Handles remain.
Independent overlapping annotations still combine visually; they are not silently merged.

Existing saved per-character highlights are NOT silently rewritten. Tap an app-created one and
press Uniformiser beside Citation to update its appearance as an undoable local edit. Recolor
or resize also uses the new appearance. Merely opening/tapping a PDF does not bulk-rewrite it.
Already exported PDFs remain unchanged. Export again to obtain the new appearance externally.

## Next functional step — editable Contents
With an app-managed highlight selected, Citation opens a text editor. Appliquer commits manual
PDF Contents as a durable local edit, covered by Undo/Redo and Save As. Moving handles keeps
manual Contents exactly, including decomposed Unicode/newlines/empty text. Automatic extraction
remains recorded separately. Notes remain separate annotations; no notes UI added here.
Manual text draft is held across rotation/dialog retry, scoped to the selected annotation;
process-death recovery of an UNAPPLIED draft is not claimed. Applied text has normal checkpoint
recovery. Length is bounded to100000 characters. No automatic document/content transmission.

## Delete latency — bounded claim
Do not pretend a durable delete completed before it did. Native candidate save/validation,
checkpoint sync/checksum and rendering remain required. Maintenance/pruning is moved AFTER
UI result delivery on the serialized worker. Queue/commit/render/cleanup milliseconds are logged
under PdfEditTiming without URI/name/text. No instantaneous-deletion guarantee; measure the
remaining delay on the tablet. Reduced native quad count can reduce appearance work, but no
performance improvement is claimed without device measurements. No safety checks disabled.

## Tablet checks
1. NEW highlight over the screenshot's orange title and justified yellow body: inspect selected
   AND after Hand. Expect continuous same-row coverage without per-character dark stripes.
2. Include the three dots/ellipsis in the right paragraph. Check coverage and Citation text.
3. Existing app highlight: tap, Uniformiser, Hand; then Undo/Redo and verify ID/count unchanged.
4. Export and inspect another reader: smooth appearance, colors, quotation, original unchanged.
   Separate overlapping annotations may still overlap; this update does not merge them.
5. Citation: correct text with accents/newlines, Appliquer. Move BOTH handles. Manual text must
   remain exact. Undo/Redo correction and resize. Export and verify Obsidian/Docear extraction.
6. Reopen original source and app-exported copy; confirm selectable handles, colors and corrected
   Contents. Retest because the previous "IJ" result was unclear.
7. Delete a highlight and observe delay; compare with queue/commit/render/cleanup timings if noticeable.
   Retest repeated edits/pinch/rotation. Never treat busy feedback as a completed save.
8. Keep third-party/locked/unrecognized highlights untouched. General imported editing and
   original overwrite remain pending. M3j Save As behavior is unchanged.

## Runtime/author verification
No Java/Kotlin/Gradle/PowerShell/Android runtime is available to the author. Source anchor,
lexical/XML/resource and independent reference checks are not compilation/JUnit/Compose/JNI.
Installer checks the ACTUAL corrected M3k source receipt before writing, then executes build,
JVM/Lint and20 suites on Windows/tablet. New native suite verifies1 row quad vs many logical
quads, handle restoration after native save/reopen, manual Contents, deletion byte-exact Undo
and original preservation. Existing M3k native copy-based resize tests are updated to carry
appearance along with logical geometry. No test is skipped or deleted.
