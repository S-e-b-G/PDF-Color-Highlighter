# M3d - responsive, continuous read-only selection feedback

## Accepted starting point
User reports M3c.1 successful build/Lint/install, 244 JVM tests (zero failures/errors/skips),
eight instrumentation suites PASS. The troublesome column now selects correctly. Diagnostic
changed 123 -> 116 rows with the same 7134 physical glyphs, confirming supported-row merging.
Known issues: cross-column reading continuation unsupported; drag feedback lags; screenshot
shows unpainted justified-word gaps. This increment addresses feedback, not reading order.

## Code changes and why

1. Immutable geometry caches
Glyph quad bounds/area, glyph text/whitespace and line bounds are calculated once rather
than rebuilt during repeated hit/range/paint work. Inputs are the existing copied immutable
DTOs; no native Page/StructuredText/Font is retained. This trades some snapshot memory for
less allocation. Native glyphs/codepoints/quads/quotation normalization do not change.

2. Bounded exact hit index
A 128-bin vertical index filters candidate lines, then glyph bounding boxes, then the SAME
actual quad containment test. Original first-hit order is preserved, including overlaps.
Index limits: <=20000 lines and <=200000 bin references. Oversized/invalid-height cases
fall back to a no-extra-array linear scan. Only a weak single-entry prepared index is cached:
it may be collected and rebuilt under memory pressure; it does not keep old documents alive.
No native work occurs per touch. Margin assistance and flow/column rules remain unchanged.

3. Progressive feedback without reviving stale gestures
Previously, a MOVE result was dropped whenever another MOVE arrived before calculation
completed; a fast drag could therefore show nothing until it paused. Keep one running and
one latest pending request, but permit VALID, in-order interim MOVE ranges within the SAME
gesture epoch. Do not publish obsolete refusals/errors as interim feedback. Pending release
suppresses old MOVE feedback; final results stay strictly latest-only. Cancel/page/mode
changes increment the epoch; ViewModel retains its snapshot identity/loading/tool gates.
Worker completion still serializes dispatch. This does not allow obsolete document/page/
pinch-canceled results or old final releases to publish. End result accuracy is not relaxed.

4. Paint-only continuous bands
The screenshot gaps arise because inferred textual spaces intentionally have no native
character quad. Painting separate glyph quads leaves those spaces white. M3d caches UI-only
PreviewPaintBand values and paints continuous same-row bands across small word gaps.
These bands are NOT TextGlyph/GlyphQuad/PDF annotation objects and never replace or mutate
SelectionSpan.glyphs/quads. Native annotation/export geometry is untouched. A future native
annotation feature must not reuse paint rectangles as a substitute for verified QuadPoints.

Bands require similar height, near-matching top/bottom, monotonic row direction and gap
<=1.5 maximum glyph height. Different rows and wide gutters remain separate. The measured
6.8-6.91 word gaps at about 7.32 height fit; the 26.96 column gutter does not. This is bounded
LTR preview painting, not universal rotated/table layout. Never paint one large bounding
rectangle across a multiline paragraph or columns. Existing Canvas/Compose clipping stays.

APK/status version is M3d. No dependency/wrapper/SDK/local.properties/Git/workspace changes,
PDF mutations, editing history, saving/recovery or automatic cross-column selection added.
Existing note/save/history/settings controls remain disabled. Existing physical-row and
Unicode safeguards, color/Hand/pinch/reading-state behavior must remain.

## Delivery and source protection
ZIP includes complete new files, reviewed exact-match patches.json and guarded installer.
The installer constructs/writes complete changed files on the PC; no snippets need pasting.
Every find must match exactly once. Unrelated file text is preserved. Baseline source hashes
are captured during preflight and rechecked before writes to detect intervening edits.
Version metadata is a single exact token change, not a fragile whole-XML baseline hash;
other resource text/newline representation is preserved. Safety sources remain hash-guarded.
New-file collisions stop. Docs/status/decisions/agents/requirements merge with commit work.
Backups/created-file records are made; local write failure attempts targeted rollback.
A later build/test/device failure leaves source applied. Do not reapply: retry Validate-M3d.
Post-apply changed-source hashes are stored in .validation/M3d-applied.json for the validator.

Extract separately; run Apply-M3d.ps1 -ProjectRoot ... -BuildAndInstall. -ValidateOnly edits
nothing; -AdbPath overrides discovery. No tool installation/upgrades, data clear or uninstall.
The SDK issue was explained by another Windows session; use the working SDK session or
correct machine-local sdk.dir, never rerun earlier application/manifest repair scripts.

Expected JVM total: 268 = accepted 244 + 24 hit-index/paint/progressive-queue tests.
Validator builds/tests/Lints, summarizes XML, installs with -r, reruns the eight accepted
suites and adds PASS: selection feedback. Exactly one authorized connected tablet.
The new suite paints synthetic ranges into a software Canvas to test word-gap fill, gutter
separation, clipping and untouched real quads. It does NOT benchmark real finger latency,
Compose frame rate, GPU behavior, huge documents or cross-column reading order.

## Five tablet checks
1. Confirm M3d. On the same dense article page, drag slowly and continuously, then faster.
   Valid intermediate ranges should appear during the drag rather than only after stopping.
   After release, Citation must match the final range. Report any perceived lag/stuttering;
   no hard timing improvement is promised without measurement.
2. Repeat the justified line from the screenshot: normal word gaps within the selected
   part should be painted continuously. Start/end should not expand to unrelated words.
   Native quotation spacing and accents must stay correct.
3. Cross several lines: one band per selected physical row, no filled vertical paragraph
   rectangle or column gutter. Try a partial first/last line; margins remain unpainted
   outside the actual selected range (except normal word snapping).
4. During selection add a second finger/pinch, cancel, change color/Hand, change page/PDF
   and rotate. No stale overlay/quotation from a canceled gesture/page/document may return.
   A final rejected range should clear/refuse as before; obsolete interim refusals should
   not flash over newer valid feedback.
5. Recheck Fit/refinement alignment, clipping, recents/properties/page jump. Selecting from
   column 1 to column 2 still refuses by design: this requires a separate verified reading-
   order increment, not arbitrary diagonal merging. No native highlight is saved.

Return build/JVM/nine-native summary plus latency/gap observations. The current previews
are UNSAVED. After feedback acceptance, plan verified column/reading-order and endpoint
work separately, then mutations with completed-edit durability safeguards. Do not call
full selection/annotation/history/save/recovery complete.

## Author evidence boundary
No Java/Javac/Kotlin/Gradle/PowerShell/Android SDK/ADB is available in this author runtime.
No compilation/JUnit/Lint/PowerShell/native/device pass is claimed. validation.json records
static/package/reference checks only. 268 is expected, not an observed passed report.
Actual speed/frame time/heap impact must be measured on the tablet; weak-cache collection
and extra geometry-cache memory mean no hard performance or process-memory guarantee.
