# Project workflow
- Communication, code, comments and documents: English. Runtime UI: French resources.
- Current accepted increment: M3a read-only structured-text/selection proof. Do not claim interactive annotation or save completion.
- Android 10 minimum: API 29. Validate Lenovo Android 11 and Teclast Android 10 separately.
- One MuPDF engine, pinned at 1.28.5. Engine changes, second engines and paid dependencies require approval.
- Serialize native operations; never dispose a native document while a queued operation is using it.
- Do not transmit documents, add INTERNET permission, or log quotation/comment/password content.
- Highlight Contents are editable quotation text. Additional comments use separate Text notes.
- Manual Contents must survive geometry edits. Never silently resynchronize corrected text.
- Tablets: one primary toolbar row in both orientations; all 12 color tools directly visible.
- Do not hide colors, scroll the primary toolbar, shrink touch targets, or move primary actions without approval.
- The accepted read-only reader is not an annotation editor. Keep mutation controls unavailable until annotation interoperability and safe completed-edit persistence are implemented.
- Complete files, small increments, real tests; no fabricated output or empty feature handlers.
- Do not commit private PDFs, local paths, signing keys, or unrelated changes.
- Update docs/STATUS.md and requirements.csv at each increment; distinguish host results from user/device results and do not claim Teclast acceptance without evidence.


## Current addendum — M2b.2
Supersedes older milestone/toolbar status, not safety/licensing constraints.
- M1.1 fixture interoperability passed; M2b.1 host/device checks passed by user report.
- M2b.2 supplied source has not been compiled/run by the author.
- Top: six highlights / Save Undo Redo SaveAs / six notes, 48dp slots.
- Bottom: Open name / FitPage Previous page-total Next FitWidth / Hand Settings.
- Preserve native/Compose clipping, JNI search/bounds workarounds and private-text encoding.
- Small-page horizontal pan is explicit/bounded; rotation preserves it; Fit recenters.
- Page validation and press feedback must not activate unavailable tools or delay commands.
- Production editing/save/history/settings, recents/retained session and sharper rendering are pending.


## M2b.4 addendum
Toolbar feedback is now 1000ms, superseding prior 2000ms notes. Page navigation retains fit-relative zoom and proportional horizontal placement, starts at top, and persists these viewport hints through rotation. Editing and native-source safety rules remain unchanged.


## M2c.1 addendum
Filename opens cached read-only properties; native metadata stays on the serialized worker. PDF dates and source-provider mtime are distinct, and missing values are not fabricated. M2b.4 navigation/feedback/clipping remain intact. Recents/restart location and retained native session are still pending.


## M2c.2 continuity
M2c.1 accepted by user; M2c.2 source supplied but not compiled/executed by author.
Expected JVM total 94. Follow docs/M2c-2-ACCEPTANCE.md and await device results.
ReaderViewModel retains the read-only native session; never retain Activity/View.
Recents/location use serialized bounded AtomicFile metadata, not PDF recovery.
Preserve all M1.1 JNI/encoding/manual-Contents policy, toolbar layout, clipping,
gesture geometry, 1000ms feedback and properties behavior.
Ordinary launch shows recents; reopening restores Y; page navigation starts at top.
Do not delete PDFs/recovery data or revoke grants on list eviction/removal.
Next after acceptance: durable working/save foundation; sharper rendering separately.
Editing/history/save/autosave/recovery/settings remain unavailable.


## M2c.3 continuity
M2c.2 accepted: user reports 94 JVM tests, both instrumentation checks and all
manual tests OK. M2c.3 source delivered, author Android/Kotlin execution pending.
Expected JVM total 114. Use docs/M2c-3-ACCEPTANCE.md and await device acceptance.
Native source now uses a synced/verified immutable baseline in noBackupFilesDir,
not cache. Exact source-byte identity is persisted; no original PDF writes.
Retention is ONLY for clearly marked unedited baselines. Never apply it to edits,
journals or future working files; preserve unknown/extra-file material and live
sessions. Fingerprints do not imply automatic conflict handling or edit recovery.
Keep MuPDF APIs/JNI workarounds, quotation policy, toolbar/geometry/gestures,
properties and M2c.2 recents/retained lifecycle unchanged.


## M2c.4 continuity
M2c.3 accepted: 114 tests and three instrumentation PASS labels, user report.
M2c.4 expected 136 JVM tests; author Kotlin/Android execution pending. Follow the
M2c-4 acceptance guide. Native logical dimensions own geometry, not bitmap pixels;
same-page refinement must not reset gestures or saved anchors. Keep one outstanding
request, serialize native work and disposal, separate refinement/navigation tokens,
reject stale tickets, check bitmap bounds before allocation, never immediately recycle
published images. No unbounded cache, engine/build/settings changes or PDF writes.
Keep all M1.1 interoperability/manual-Contents and M2c.3 workspace safeguards intact.


## M3a continuity
M3a was accepted by the user on 2026-10-21. The user reported 161 JVM tests with zero
failures/errors/skips, five instrumentation PASS labels, and successful reader checks on
the Lenovo Android 11 tablet. A separate host command in this workspace completed
`:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug`; Gradle marked the
assemble/test tasks UP-TO-DATE and executed Lint. The existing JUnit XML totals 161 tests
with zero failures/errors/skips. Lint reports 22 warnings and 3 hints.

The reader UI remains read-only. The M3a native adapter returns copied glyph data only;
honor copy permission and serialized ownership, destroy StructuredText, and never retain
native objects in UI state. Selection is conservative LTR same-block/run with actual
quads; it does not infer cross-column/paragraph or complex-cluster text and does not
invent quotations from blank space. The proof is not wired to production touch. Keep
manual highlight Contents unchanged and separate from Text notes. No PDF writes,
production annotations, editing/history/save/recovery completion, or M3b work is claimed.


## M3b continuity
M3a accepted: 161 JVM tests and five instrumentation PASS labels, user report. M3b source
expected 182 tests; author runtime execution pending. Read docs/M3b-ACCEPTANCE.md.
Six left colors now control explicit read-only previews, not real highlights. Preserve
all tool slots/order/48dp targets. Snapshot extraction stays on native owner worker,
pure selection uses latest-only background queue. Reject stale page/tool/cancel results.
Map native origin and overlay actual quads using the same placement as pixels. Never
write PDFs or mark recovery/save/history complete. Page/background/rotation discard
preview/tool state. Preserve M3a Unicode/column/manual-Contents and M2c.3/4 safeguards.


## M3b.1 continuity
M3b automated checks passed (182 tests, six native labels), but manual multiline and
margin endpoints FAILED. M3b is not fully accepted. M3b.1 expected 206 tests and seven
native labels; author runtime execution pending. Read docs/M3b-1-ACCEPTANCE.md.
Selection endpoints can resolve to actual glyphs within text-line bands on the sheet;
raw hit stays strict. Native block IDs are not hard selection boundaries; geometric
path/column/ambiguity/gap guards govern continuation. No new UI, rendering, annotation,
PDF write, history/save/recovery claims. Require failed-passage regression acceptance.


## M3c continuity
New source is diagnostics/version, NOT a paragraph/cross-column selection fix. Known
same-paragraph false refusals and paragraph/reading-order limitations remain explicit.
Expected 216 tests from source baseline 206+10; author runtime execution pending. Read
M3c-ACCEPTANCE.md. Reports contain only structural DTO metadata, not text/identity/bytes;
never automatically export/log/transmit/copy them. Preserve user consent and review.
Update APK versionName AND reader_increment resource for future app increments. Keep
fixed status height, permanent tool layout, all existing selection/render/persistence
safety rules and source guards. Do not advance PDF mutations without durability safeguards.


## M3c.1 continuity
M3c passed user host/native checks (216 tests, seven suites). M3c.1 source expected244
JVM tests and eight native suites, author execution pending. Read M3c-1-ACCEPTANCE.md.
Physical-row coalescing must preserve all real glyphs/quads, same-column guards and
Unicode/manual-Contents policy. Inferred spaces are metadata, never fake glyphs/geometry;
word and character selection must respect them. Keep two raw supporting rows, bounded
normalization, idempotence and no cascading supports. Native cleanup precedes pure
normalization. No PDF writes, read-order-column continuation or semantic paragraph
claims. Retest original user layout; keep APK/status increment synchronized.


## M3d continuity
M3c.1 passed user host/native checks (244 tests/eight suites) and physical-row manual case.
M3d source expected268 tests/nine suites; author runtime checks pending. See M3d acceptance.
Cache only copied immutable data; weak/bounded hit index must preserve exact first-hit order.
Interim feedback is valid in-order MOVE only, same gesture epoch and VM snapshot/tool/page;
never revive canceled/page-old/final-old results or suppress final release accuracy.
PreviewPaintBand is UI-only, NOT a native GlyphQuad or PDF annotation format. Never use
paint rectangles to replace native selection geometry/export QuadPoints. Preserve gutters,
row separation, native ownership, physical row word metadata and manual Contents policy.
No real PDF mutations, completed save/recovery, automatic column hop or speed guarantee.
Keep APK versionName and reader_increment synchronized. No automatic Git operations.


## M3e continuity
Expected 298 JVM tests/ten native suites; author runtime verification pending. User M3d
manual feedback positive, but no full post-fix automated log supplied. Verify M3d receipt/
source/bare-return fix before applying. See M3e-ACCEPTANCE.md.
Handle adjustment is character/cluster-safe with exact fixed logical endpoint; do not
re-hit-test the opposite point and drop zero-area punctuation. Retain shared range/flow
core and explicit queue intent, epoch/snapshot/release gates. Handle tap no-op; slop/grab
offset/cancel/pinch/rotation behavior must pass. UI punctuation height is paint-only and
context-bounded. No letters/quads/PDF edits fabricated. No permanent toolbar changes,
column-reading-order/auto-scroll/accessibility/editor/history/save/recovery completion.
