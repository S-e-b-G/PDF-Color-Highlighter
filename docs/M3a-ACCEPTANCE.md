# M3a - read-only structured text and selection proof

## Accepted starting point
M2c.4 is accepted by user report: Gradle 9.4.1/JDK 17 build/Lint/install success,
136 JVM tests with no failures/errors/skips, clipping/recent-storage/durable-workspace/
adaptive-render instrumentation PASS, and rendering/manual reader checks OK.
No new complete source export is needed: M3a adds independent selection code and
instrumentation only. The existing reader source is not replaced or reconstructed.

## This increment delivers
- Pure page/glyph/quad snapshots with real scalar code points and native page coordinates.
- A MuPDF 1.28.5 adapter using StructuredText.getBlocks(), TextLine.dir and TextChar
  code point/quad/bidi/flags. StructuredText is destroyed before returning the copied DTO.
- Actual convex-quad hit testing, not a nearest word or arbitrary rectangular overlay.
- Forward/reverse range selection with outward word snapping and a character-level mode.
- Preservation of surrogate-pair characters, combining marks, simple emoji modifiers,
  literal ligatures, French accents/apostrophes, internal NBSP and visible hyphens.
- Separate visual line breaks and normalized automatic quotation, using the existing
  M1.1 QuotationNormalizer. Manual highlight Contents are NEVER passed to this code.
- Conservative inline large-gap/wide-space splitting and refusal to cross native text
  blocks, run IDs, disjoint columns, large vertical gaps or unsupported text lines.
- Explicit empty/copy-restricted/too-complex/no-hit/unsupported results; no fake quotation.
- 25 new JVM tests and a real native synthetic three-page proof.

## Deliberate scope boundary
This is a runnable selection-engine proof, NOT interactive selection in the reader.
No current toolbar, gestures, renderer, reading state or document workspace is changed.
The 12 annotation tools remain disabled. No highlight/note is created, no PDF is saved,
no history/recovery feature is claimed complete. The native adapter is exercised by
instrumentation now and is reserved for M3b's production serialized-session integration.
This is explicit technical validation scaffolding, not a hidden final implementation.

## Conservative limitations to validate before expanding
French/English horizontal LTR is the target. RTL/bidi, vertical/rotated line direction,
CID/GID placeholders, invalid/control/replacement characters and complex ZWJ/flag emoji
are rejected rather than split or misquoted. A simple ligature remains one native glyph;
character adjustment cannot subdivide it into fictional character geometry.

Native text blocks are conservative flow boundaries, NOT proof of semantic paragraphs.
Selection across different blocks is currently refused, even if a human sees one paragraph.
Same-block lines must progress downward, remain reasonably close and horizontally overlap
or share a left edge. Inline gaps/wide whitespace over max(12 page units, 2.5 line heights)
start another run. These are engineering thresholds, not universal layout inference.
Some unusual documents or short/ragged lines will be conservatively refused. No general
reading-order, column, paragraph or grapheme-cluster algorithm is claimed complete.

No OCR; an empty layer yields EMPTY. A bad but valid Unicode map cannot always be detected.
The 100,000-glyph limit bounds copied snapshot payload, NOT allocations inside MuPDF or
its Java getBlocks implementation, which materializes blocks first. Native extraction/
selection latency and peak heap need measurement before running this on arbitrary pages.
PageTextSelector is linear in page/selection size, no spatial index yet. All future native
calls must stay on the owning serialized session worker, honoring Document.PERMISSION_COPY.
Never put native Page/StructuredText/Font references into ordinary UI state.

Snapshot coordinates include the native page origin; UI integration must use one tested
transform that subtracts pageBounds.left/top and applies the accepted viewport placement.
Do not infer screen coordinates from PDF points or the current bitmap resolution.

## Installation and automated checks
Extract the ZIP into a separate update directory. Use the normal explicit project-root
PowerShell command with Apply-M3a.ps1 -ProjectRoot ... -BuildAndInstall.
-ValidateOnly performs preflight only. Build/device retries use Validate-M3a.ps1; never
reapply after source application. -AdbPath can override ADB discovery.
No dependencies/locks/Gradle/JDK/VSCode changes, uninstall, data clear, Git operations or
original PDF writes. Exact guards protect the unchanged engine/build/normalization code
available from the source export; M2c.4 source markers/test count verify the newer baseline.
The debug manifest has an exact M2c.4 normalized hash guard. Existing reader code is untouched.
Docs/STATUS/DECISIONS/AGENTS/requirements are merged to preserve baseline-commit updates.
A backup and created-file list are made before writes. Write-phase failures attempt
rollback; a later build/test/device failure leaves the source applied for diagnosis.

Expected JVM total: 161 = accepted 136 + 25. This is not an observed passed report.
The validator builds/tests/Lints, summarizes JUnit XML, installs with -r, reruns the four
accepted instrumentation suites and adds:
  PASS: structured text selection

The new suite writes only its own synthetic test PDF in a unique cache filename, checks
French word/multiline/reverse geometry, refuses a two-column crossing, verifies a restricted
request returns no text and an empty page returns EMPTY, uses DTOs after native cleanup,
checks the original synthetic bytes remain unchanged, then deletes that test file.
The suite does not access private PDFs. No new Docear/Obsidian extraction test is needed:
there are no new saved annotations in this increment.

## What to return
Return the JVM summary and five instrumentation result labels, or the first relevant error.
The ordinary reader intentionally looks unchanged. Briefly check that an ordinary PDF still
opens and that zoom/pan/next/recents work; no new private PDF upload is needed.
After acceptance: M3b wires the validated snapshots into an explicit read-only touch-selection
preview with tested native-to-view transformations. Then add real highlights/history and
completed-edit persistence safeguards. Do not skip safe persistence before enabling mutations.

## Evidence
Author environment has no Java/Javac/Kotlin/Gradle/PowerShell/Android SDK/ADB. No compilation,
JUnit/Lint/PowerShell/native/device result is claimed here. validation.json records static/
package and independent Python fixture/reference checks only. Tablet execution is the gate.
Exact Java tag source inspected:
https://github.com/ArtifexSoftware/mupdf/blob/1.28.5/platform/java/src/com/artifex/mupdf/fitz/StructuredText.java
blob d9c5c178ecfc7450c8215e403e89ca82aea8a93e
StructuredTextWalker.java blob 91e6f7cd5151859b628993552146efe46ebd2977 was also inspected;
the implementation uses getBlocks and does not introduce a custom walker.
