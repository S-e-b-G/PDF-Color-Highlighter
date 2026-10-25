# M3c.1 - evidence-based physical-row reconstruction

## Accepted baseline and diagnosis
User M3c report: build/Lint/install successful, 216 JVM tests with zero failures/errors/
skips, all seven native suites PASS, visible M3c version OK. Selection layout limits remain.
The content-free diagnostic identifies a concrete row-fragment ambiguity: original report
L10-L17 share y=282.86..290.18, block=2/run=0, span x=40.48..283.44, contain 53 actual
characters, and are separated horizontally by about 6.80..6.91 page units. These are
fragments of one justified physical row, not eight unrelated next-line choices.
The old path refuses multiple next-row candidates, explaining any range crossing this row.
Last pointer endpoints were not captured, so not every reported refusal is proven to use it.

A second concrete boundary: original L59 bottom=669.14 to L60 top=676.78 gives gap 7.64,
about 1.04 times maximum line height 7.33. The old cross-ID limit 0.9*7.33=6.60 refuses it.
The actual column gutter is about 26.96, much larger than these word gaps. No original
PDF, copyrighted article text or document identifier is copied into these fixtures.

## Changes
1. Normalize supported same-baseline fragments after native cleanup, once per page.
   Fragments must share native block/run, similar heights/baseline, reliable LTR data,
   small non-overlapping horizontal gaps and TWO distinct nearby full-width support rows
   in that same block/run. One broad heading is insufficient evidence to join columns.
2. Preserve actual glyph objects/quads. Missing word spaces inferred from visible gaps
   are text-boundary metadata only, never invented glyphs/rectangles. Existing native
   whitespace is not duplicated. Word snapping stops at inferred boundaries; character
   mode remains character-based. Glyph counts/geometry are not inflated by textual spaces.
3. Cross-block vertical gap limit becomes 1.25 line heights (was 0.9), covering the measured
   1.04-height case. Other height/alignment/ambiguity/column limits stay. Same-ID limit 2.5
   stays. No global blank-page snapping or automatic column-1 to column-2 traversal.
4. APK/status/diagnostic version is M3c.1. No toolbar/UI controls, reader geometry, rendering,
   worker queues, workspace retention, PDF writes, annotation/history/save/recovery changes.

This is geometric physical-row reconstruction, NOT a semantic paragraph parser. The current
visual-line quotation normalizer is preserved; new semantic paragraph separators are not
invented solely from block IDs or gaps. Universal tables, typography, all reading orders,
OCR or all future documents are not claimed supported. Unsupported/complex cases can refuse.

## Limits
Coalescing: <=2048 input lines, same block/run and reliable data; baseline top/bottom delta
<=0.2 maximum row height; height ratio <=1.35; gap in [-0.15,1.25] row heights. At least two
distinct supporting single-source rows within 3.5 heights cover the proposed envelope.
Support rows with inferred spacing or sourceFragments>1 are not reused, preventing a
normalization feedback loop. Leading combining/modifier fragments are not relocated.
Text-space inference requires gap>=0.15 row height and no native whitespace at the join.
These evidence-guided conservative thresholds are not universal semantic guarantees.

Large gutters, differing block/run, unreliable direction, overlap, no support or sparse
fragment-only pages are left unchanged rather than guessed. The supplied report's row
satisfies these criteria; its 8 records should become one physical row if the live extraction
matches the report. Page line count can drop while physical glyph count stays unchanged.
For that one cluster alone 123 rows would become 116; this is a prediction, not device evidence.
Diagnostics now describe normalized physical rows, so old row numbers after L10 can shift;
use native bounds and block/run IDs to compare. The report format intentionally stays stable.

Existing M3a snapshot budget 100000 glyphs and M3b visible range budget 5000 remain.
Native getBlocks allocations and total process/heap/GPU memory are not bounded by these.
Coalescing runs after StructuredText destruction on the existing owner thread; no native
object is stored in snapshots. Normalization is idempotent. Latency needs tablet measurement.

## Apply/build/install
Extract separately. Apply-M3c.1.ps1 -ProjectRoot ... -BuildAndInstall checks exact source/
payload hashes, baseline test declarations and unchanged safety files, makes backups,
builds/tests/Lints, installs with -r, runs eight native suites, then launches the reader.
-ValidateOnly edits nothing. After source application, retry Validate-M3c.1.ps1 instead of
Apply. -AdbPath overrides discovery. New-file collisions/source drift stop before writes.
Docs/status/decisions/agents/requirements merge with local commit documentation.
Write-phase failures attempt targeted rollback; later build/device failure leaves source.
No dependencies/wrapper/locks/JDK/VSCode changes, Git operations, uninstall/data clear,
source-PDF writes, automatic uploads or report exports. Never bypass hash guards.

Expected JVM total: 244 = accepted 216 + 28 new row/spacing/selection tests.
New native result: PASS: physical text rows. The seven accepted suites also rerun.
The new suite uses generated text, real native glyph quads, deliberate fragmentation/
omission of whitespace in a COPIED DTO to stress reconstruction, word-boundary and
reverse checks, source immutability and cross-column rejection. It does not claim that
MuPDF will fragment that synthetic PDF exactly like the user's producer. No private PDF.

## Four tablet checks on the same page/passages
1. Verify M3c.1 is displayed. Select through the original y=282.86..290.18 row (original
   diagnostic L10-L17), including preceding/following lines. Compare full Citation.
   Words must have spaces, not be concatenated. Tapping/dragging within a single word
   should not expand to the whole reconstructed row.
2. Repeat reverse and at another zoom. Select across the paragraph boundary represented
   by original L59->L60. Report any remaining refusal; no blanket success is assumed.
3. Check Diagnostic: the fragmented physical row should be coalesced; glyph count should
   not increase. Compare bounds/block IDs rather than expecting the old row numbers.
4. Recheck column isolation, large gap/gutter refusal, Hand/color exit, pinch cancellation,
   alignment, page/document/rotation resets. Column reading-order continuation is still
   not implemented. Everything remains an UNSAVED preview, not a PDF annotation.

Return build/JVM/eight-native summary and these cases. A reviewed structural diagnostic
and description of the intended span is enough initially; no source article upload needed.

## Evidence boundary
Author lacks Java/Javac/Kotlin/Gradle/PowerShell/Android SDK/ADB. No compilation/JUnit/Lint/
native/device pass is claimed. validation.json records static/package/reference checks.
244 is expected until user's run. Do not accept selection globally or enable mutations
until remaining real-document layout cases and completed-edit persistence are resolved.
