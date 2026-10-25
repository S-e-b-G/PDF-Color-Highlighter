# Accepted requirement updates and engineering defaults
## 2026-10-10: editable highlight Contents
User requirement: the copied selected text is editable; additional comments
belong in independent notes, not appended to the highlight's own Contents.
No quotation/comment separator is used.
Engineering default: new highlights begin in AUTOMATIC mode. A changed Contents
edit switches to MANUAL. Geometry edits update the extracted quotation but leave
manual Contents unchanged. Explicit replacement with extracted text returns to
AUTOMATIC. Imported arbitrary Contents begin in MANUAL. No private metadata is
yet written; persistence of this mode will be tested in Milestone 1.
This avoids erasing spelling/OCR/extraction corrections. The domain policy and
seven tests are supplied, but the editor and annotation mapping are not built.
## 2026-10-10: tablet primary toolbar
One row for tablets in portrait and landscape. Vector icon actions are allowed.
Document name, navigation, page count, Fit and Save As may occupy another row.
All six highlight and six note selectors remain visible without scrolling.
Twelve color targets alone need 576 dp at a 48 dp target width. Eighteen primary
targets need 864 dp before spacing/insets. Icons reduce visual width, not the
recommended touch target width. Fit remains unresolved on actual tablets.
Do not silently compress, overlap, overflow, scroll or wrap tablet primary tools.
If real width is insufficient, request approval for a specific adjustment before
implementing the final toolbar. Phones may use several rows.
## 2026-10-10: minimum Android version
minSdk = 29 (Android 10), superseding proposed API 30. Lenovo Android 11 and
Teclast M40 Android 10 are separate device acceptance targets. No ABI filter is
imposed until the AAR and device ABI lists are inspected.
## Smoke implementation scope
Compose + Material 3 and Activity Result SAF opening. A plain dedicated executor
owns read-only native operations. Activity-owned lifecycle handling is limited
scaffolding, not the final retained ViewModel session architecture.
An explicit rotation saves URI/page in instance state and reopens them. There is
no reading-location persistence across ordinary app restarts yet.
Cache copies are allowed because this increment makes no edits; future unsaved
recovery must use durable filesDir storage, not this smoke cache.
The original URI is never opened for writing.


## 2026-10-18 — M2b.2 accepted navigation revision
Top: six highlights / Save Undo Redo SaveAs / six notes, all 48dp.
Bottom: Open name / FitPage Previous page-total Next FitWidth / Hand Settings.
Center block is 272dp with equal flexible sides.
Small pages center until explicit horizontal pan; bounded free-pan state survives rotation; Fit resets it.
Page input is 1-based and validated; native page navigation stays 0-based.
Enabled toolbar commands have nominal 2s fading feedback without execution delay.
SaveAs uses a project-authored disk/pencil icon; no linked image is copied.


## 2026-10-18 — M2b.4 page-navigation continuity
Previous/Next retain the fit-relative zoom and normalized horizontal travel fraction, and start at top.
Page jumps share that behavior. Opening a different document and explicit Fit Page retain centered defaults.
Different page widths use proportional valid travel, not a fixed pixel offset.
Enabled toolbar feedback lasts nominally 1000ms; scoped circular-ripple suppression remains.


## 2026-10-18 — M2c.1 document properties
Filename click opens a read-only, scrollable metadata dialog; no native work occurs on that click.
Read metadata once on the existing serialized document worker, cache an immutable DTO, and refresh on document open.
Keep PDF dates, provider mtime and local-copy size separate; do not invent filesystem creation time or a path from URI.
Dates retain recorded precision/timezone; unsupported date text remains visible raw. No private metadata logging.
The original cached read-only working-copy/session design is not upgraded to durable editing persistence in this increment.


## 2026-10-18 - M2c.2 reading persistence and lifetime
Use the live M2c.1 export as baseline, not historical M2c.2 delivery claims.
AndroidViewModel owns the read-only native session across rotation and holds
application context only. Activity owns/releases viewport callbacks.
Use a serialized bounded/versioned AtomicFile metadata snapshot in filesDir,
without changing the existing dependency/lock set. This replaces the preferred
DataStore implementation for this small increment, not the final PDF format.
Ordinary launch shows recents. Reopen restores full normalized viewport;
Previous/Next/jump retain accepted top-start/zoom/proportional-X behavior.
Read errors preserve evidence and suspend writes; write errors warn and retain
memory for retry. Debounce 750ms, active-movement checkpoint about five seconds,
transitions enqueue immediately, all subject to scheduling/I/O.
List removal/eviction never deletes PDFs/recovery files or revokes shared grants.
This is not durable editable storage, PDF saving or annotation recovery.


## 2026-10-18 - M2c.3 immutable durable baseline
M2c.2 manual/automated acceptance is complete by user report (94 JVM tests).
Import the source read-only into noBackupFilesDir/reader-workspaces; sync and
rehash bytes and persist exact URI/name/size/SHA-256 identity before native use.
Publish with a checked same-root rename, then gate retention on native rendering.
Retain current plus newest other known immutable baseline; never prune live,
unknown, candidate or extra-file-containing workspace directories. Dirty future
work must use separate files and revoke eligibility first. No write-back, recovery
chooser, automatic retained-copy fallback or active external-conflict detection.
Source comparisons are a tested helper only. Reader UI, geometry, lifecycle and
engine/dependency versions remain unchanged. See docs/M2c-3-ACCEPTANCE.md.


## 2026-10-21 - M2c.4 bounded adaptive whole-page rendering
Keep preview and refine after 250ms idle/no active page touch. Geometry is based on
native page dimensions, not current bitmap pixels. Same-page pixel swap does not reset
pointers/anchors or write reading state. One outstanding refinement, serialized native
work, separate refinement token from navigation generation, stale-delivery tickets.
Check transformed RectI bounds before allocation: max 4096 per side, 8,388,608 pixels.
No multipage cache, immediate recycle of published images, unbounded zoom sharpness,
forced GC or whole-process memory guarantee. Keep preview plus French warning on
refinement failure; retry on next page/open. No engine/build/workspace-retention/PDF
write changes. Preserve M2c.3 read-only workspace and future dirty-work safeguards.


## 2026-10-21 - M3a read-only structured text and selection proof
Use actual MuPDF 1.28.5 TextChar code points and quads, copied into engine-free DTOs.
Destroy native StructuredText before returning; future extraction stays on the document
owner's serialized worker and must honor copy permission. No reader wiring or PDF writes yet.
Use convex glyph-quad hits, deterministic endpoint order, outward word and cluster-safe
character modes. Preserve literal ligatures/internal NBSP. Normalize automatic visual wraps
with existing QuotationNormalizer; never transform manually corrected annotation Contents.
Conservatively restrict selections to native block/run and plausible LTR spatial flow;
reject unsupported RTL/vertical/complex clusters and large gaps rather than invent text.
Native blocks are NOT reliable semantic paragraphs; general layout/grapheme inference is
not claimed. The copied glyph limit does not bound earlier native/getBlocks allocations.
M3b follows acceptance: native-to-view mapping and explicit read-only touch preview before
mutations. No fake annotation activation, empty handlers or final editing completion claims.


## 2026-10-21 - M3b interactive read-only selection preview
Use the six existing left color targets as clearly labelled preview tools, without
changing toolbar order/target sizes or pretending native annotations were created.
Native extraction is serialized after preview publication, with permission checks and
stale page-generation rejection. M3a DTOs remain engine-free. A separate pure worker
and latest-only queue cap work to one running plus one pending selection calculation.
Map native page origin explicitly; draw temporary actual glyph quads under Canvas clip.
Same-page raster swaps retain alignment/gesture state. Citation is a contextual overlay
button, not a viewport-resizing toolbar; quotation dialog is read-only and unsaved.
One-finger preview selects, Hand restores pan; second finger cancels pending selection
and permits pinch. Page/background/rotation return to navigation and discard previews.
No reading metadata stores tool/selection state. Limit visible ranges to 5000 glyphs,
refusing rather than truncating. No native annotation, dirty revision, history or save
claim. Next mutations must include completed-edit persistence safeguards.


## 2026-10-21 - M3b.1 selection flow and endpoint margins
User M3b tests exposed a need for line-margin endpoints and multiline continuation.
Keep raw glyph hit testing strict; assist selection endpoints only on the sheet in
nearby text-line bands, refuse remote blank areas/outside-sheet points/ambiguous gutters.
Native IDs are metadata hints, not hard paragraph boundaries. Follow bounded geometric
continuations across compatible IDs while rejecting incompatible columns/large gaps/
ambiguous branches. Use a path rather than raw native array order; preserve real glyph
quads, Unicode safeguards, automatic quotation normalization and all manual Contents.
These heuristics do not claim universal paragraph/column/layout inference. No PDF edits.
Retest actual failed passages before accepting M3b or advancing annotation features.


## 2026-10-21 - M3c diagnostics and visible development version
Do not conflate geometry paragraph-gap refusals, false refusals within a paragraph and
unimplemented cross-column reading continuation. Preserve column safety while obtaining
real layout evidence. Add a user-opened structural report containing only bounded numeric
metadata and status/rejection enums, never glyph text/quotes/URI/name/document bytes.
No automatic export/log/transmission/clipboard write. Capture an immutable report on the
user action; close on page change/rotation. Existing selector/engine/view state is unchanged.
Show M3c first in the existing fixed-height status strip and set APK versionName to M3c.
Keep reader_increment resource and APK versionName synchronized in future increments.
Refusal wording describes the selector enum, not an invented exact diagnosis. No native
annotation, paragraph-reading-order fix, final milestone or new native-suite claim.


## 2026-10-21 - M3c.1 physical text-row reconstruction
Use reported geometry, not a blanket column-gap relaxation. Reconstruct nearby same-
baseline fragments only with same block/run, reliable data, bounded gaps and TWO nearby
full-row supports. Keep actual glyphs/quads unchanged. Represent inferred word spaces
as spacesBefore metadata, not fabricated space geometry, and stop word snapping at those
boundaries. Preserve existing native whitespace and manual Contents policy.
sourceFragments prevents inferred rows becoming raw support for cascading normalization.
Run pure normalization once after StructuredText cleanup; snapshot addresses remain
consistent for downstream consumers. Update diagnostics expectations to physical rows.
Cross-ID vertical gap limit 1.25 heights covers the measured 1.04-height boundary; other
column/height/alignment/ambiguity rules remain. No automatic column reading-order hop,
semantic paragraph parser, PDF mutation or completed-editor claim. Version M3c.1 in both
APK metadata and reader resource. Runtime/user acceptance still required.


## 2026-10-25 - M3d preview feedback and paint geometry
Optimize existing read-only selection before enabling mutations. Cache immutable geometry/
scalar derived values, use bounded exact-hit buckets with original first-hit order and a
weak prepared-index cache. Oversized cases fall back without changing selection semantics.
Permit valid serial interim MOVE results only within the current gesture epoch, retaining
VM snapshot/loading/tool gates; suppress obsolete refusals and pending-release MOVE feedback.
Final results remain strictly latest-only. Cancel/page/mode invalidates the epoch.
Fill small inter-word gaps using separate UI PreviewPaintBand rectangles, never replace
real GlyphQuad/SelectionSpan or create PDF annotation geometry from paint bands. Separate
physical rows and gutters remain separate; Canvas/Compose clipping unchanged.
No measured latency guarantee, universal layout or cross-column reading-order support.
No native engine/dependency/SDK/workspace/Git/PDF changes. Version M3d in APK/status.
