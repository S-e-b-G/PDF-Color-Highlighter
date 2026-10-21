# M3b.1 - multiline and line-margin selection fix

## Evidence and diagnosis
M3b built/installed successfully: user reports 182 JVM tests with zero failures/errors/
skips, six instrumentation PASS labels. Single-line forward/reverse selection, quotation
accents, color/Hand, pinch cancellation and alignment/page-switch behavior were reported OK.
Manual acceptance is NOT complete: multiline and endpoints outside glyphs failed on user PDFs.

Two confirmed source restrictions match these symptoms: PageTextSelector required strict
quad hits for both endpoints; TextGeometryPolicy rejected different native block/run IDs.
The exact grouping/line spacing of the private PDF has not been inspected. This update
addresses those restrictions without claiming diagnosis of every possible failing layout.

## Changes
- Strict PageTextSelector.hit stays strict for callers needing actual quad hits.
- Selection endpoint resolution permits horizontal line margins on the PDF sheet, within
  a narrow text-line vertical band. Before/after a line chooses its first/last non-space
  glyph; an intra-line glyph gap chooses an actual glyph rather than inserting text.
- No global nearest-text search. Large vertical blank areas, outside-sheet points and
  exact gutter ties are refused. Endpoint assistance may select a real word when tapping
  its adjacent line margin; no text or geometry is invented.
- Native block/run IDs are no longer hard barriers. Compatible same-column lines can
  connect across different IDs. Geometry uses downward order, height compatibility,
  bounded gap and horizontal overlap/alignment. Different IDs use a tighter gap threshold.
- A bounded geometric continuation path replaces raw-array range order. Interleaved lines
  from another column cannot automatically enter the selected range. Ambiguous next-row
  branches, large paragraph gaps and incompatible columns remain rejected.
- Cached bounds and visited sets avoid repeated glyph-bound rebuilding during traversal.
- Unicode/word/combining safeguards, actual quad geometry and existing quotation normalizer
  are preserved. The M3b background queue, renderer, view mapping, UI, colors and gestures
  are untouched. No annotations, PDF writes, history or save features are added.

## Bounds and limitations
This remains conservative LTR selection, not general layout/OCR/paragraph inference.
Line-band tolerance is min(5 native page units, 0.35 line height). Endpoint ranking uses
horizontal interval distance plus a small vertical-center contribution; exact ties reject.
Only rows near the pointer and on the sheet are eligible; another column is not silently
included when a range crosses the gutter.

Continuation requires positive width/height, similar heights (ratio <=1.75), meaningful
downward progression (>0.35 minimum line height), and overlap/shared left alignment.
Across different native IDs the allowed inter-line gap is <=0.9 maximum line height;
same-ID flow retains the previous <=2.5-height gap allowance. These are documented
engineering thresholds, not universal paragraph recognition. Large spacing, complex or
ambiguous layouts may still refuse selection rather than misquote unrelated material.

Flow traversal examines at most 2048 candidate lines and stops at ambiguous or whitespace-
only intermediate rows. A refusal is never a truncated quote. The existing 100,000-glyph
snapshot limit and 5,000-glyph visible-preview limit remain. Performance needs real-device
measurement; this is not a hard memory/latency claim. Manual Contents remain untouched.

## Apply/build/install
Extract separately. Use Apply-M3b.1.ps1 -ProjectRoot ... -BuildAndInstall.
-ValidateOnly performs preflight only. After source application retry Validate-M3b.1.ps1,
NOT Apply again. -AdbPath can override ADB discovery. No reinstall of tools, dependency/
wrapper/lock/editor changes, Git operations, uninstall or app-data clear.

Exact normalized hashes guard the two changed domain files (unchanged since delivered
M3a) and the M3b debug manifest. New-file collisions stop before writes. Existing build/
workspace/normalization safety sources are guarded; M3b marker/test count checks establish
the newer layer without replacing reader sources. Backups and created-file records are
made. Local write failures attempt rollback. A later build/device failure leaves source
applied for diagnosis. Metadata docs/requirements are merged, preserving commit work.

Expected JVM total: 206 = current 182 + 24 new regression tests. No existing tests are
removed; old test names mentioning native-block boundaries are historical, while their
actual cross-column rejection assertions remain enforced.
The validator builds/tests/Lints, installs with -r, reruns the six previous suites and adds:
  PASS: selection flow margins

The new suite uses only a generated two-page PDF, real MuPDF glyph geometry, horizontal
line margins, forward/reverse multiline selection, and column/blank-space rejection.
It deliberately assigns different IDs to copied native lines to test the ID-independent
path, since actual MuPDF block grouping varies by producer. Synthetic input bytes must
remain unchanged. It does not access private PDFs or create annotations.

## Four manual checks using the same passages that failed
1. Same line: start just before the first word and finish after the last word, staying
   within the sheet and near the text row. Entire intended text should select. Reverse it.
2. Two/three ordinary consecutive lines in one column, including starting/ending in line
   margins. Compare Citation against the intended complete text, including accents.
3. Repeat reverse dragging and at a different zoom/Fit. Check that selected quads remain
   aligned and no stale quotation appears after changing page/document.
4. A two-column page: selecting within one column should not quote the other. A drag
   directly between incompatible columns/gutter should refuse instead of combining text.
   Hand/color/pinch behavior previously accepted must remain unchanged.

Return build/test/seven instrumentation labels and these checks. If a particular passage
still fails, report whether it is normal prose vs columns/table/formula, the number of
lines, whether both endpoints were in glyphs vs margins and the approximate zoom. The
specific message and a redacted screenshot can help; no private PDF is required initially.
Do not advance annotation features until this acceptance is resolved.

## Author verification boundary
No Java/Javac/Kotlin/Gradle/PowerShell/Android SDK/ADB is available in the author runtime.
No compiler/JUnit/Lint/native/device pass is claimed here. validation.json records static,
ZIP/hash/XML, independent geometry/flow reference and synthetic PDF parser checks only.
206 is an expected count, not an observed passed report. Device behavior remains to test.
