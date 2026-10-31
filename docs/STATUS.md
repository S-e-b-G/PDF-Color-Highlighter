# Current increment — M3h durable edit-checkpoint foundation; runtime pending
M3g.1 accepted by user: previously478 JVM tests/build/Lint success, device-only resume
verified saved reports without rerunning JVM, installed/passed15 native suites; manual OK.
Selection/reader logic stays frozen. M3h adds a separate immutable-PDF two-slot checkpoint
store, stale/idempotency/corruption/failure guards and native persisted-highlight proof.
Expected498 JVM tests (478+20),16 reader suites; author runtime unavailable. Editing UI,
history, autosave/recovery chooser and external Save/SaveAs remain unavailable. This is
required durable native-edit infrastructure, not another text-selection patch.
Read docs/M3h-ACCEPTANCE.md. Device-only retry is now built into Validate-M3h.ps1.

## Historical status below (superseded)

# Current increment — M3g.1 horizontal-flow orientation repair; runtime pending
User M3g automated results:466 JVM tests, fourteen reader suites, build/Lint/install/launch
success. Jury/list works. Supplied page author-reference range fails UNSUPPORTED_TEXT.
PDF geometry exposes a tall rotated publisher watermark centred on the body row, wrongly
attached by M3g row construction. Preserve native horizontal orientation and omit rotated
rows from incidental horizontal flow. Raw data remains; unsupported horizontal text still
refuses. No new paragraph/font/block barrier or resource change. Expected478 JVM tests and
fifteen reader suites, not run by author. See docs/M3g-1-ACCEPTANCE.md for actual retest.

## Historical status below (superseded)

# Current increment — M3g flexible same-column ranges; runtime acceptance pending
M3f.5 user says standards checks OK. New single-column paragraph/list range still refused;
Diag_3 shows a separate inline bullet and body, not an encoding or range-size issue.
M3g replaces next-line graph rejection by physical-row/endpoint-column intervals: paragraph
gaps/font changes/native IDs no longer veto selection, compact inline fragments are included.
Unknown text/copy/geometry protection remains. Preview budget25000; extraction100000.
Expected466 JVM tests (438+28), fourteen suites; four old policy assertions intentionally
updated, no test skipped. Author runtime unavailable. See docs/M3g-ACCEPTANCE.md.
No PDF edits, history/save/recovery, cross-page selection or automatic column reading-order.

## Historical status below (superseded)

# Current increment — M3f.5 complete inline year headings; runtime acceptance pending
M3f.4 user report: AFIS/INCOSE selection now works. Standards selection omits detached
(2013) and refuses two entries. Current diagnostic confirms same-baseline fragments
L32/L33 and L35/L36 and DIFFERENT_TEXT_FLOW. M3f.5 normalizes bounded identifier/year
headings with same-block spanning-body support before selection; no flow guard widening.
Expected438 JVM tests (414+24) and thirteen reader suites. Author runtime unavailable.
Retest both standards including both years and the wrapped description. Existing accents,
bullets, retention, native ownership and PDF read-only guarantees remain unchanged.
Read docs/M3f-5-ACCEPTANCE.md. This is not universal table/cross-column reading support.

## Historical status below (superseded)

# Current increment — M3f.4 native replacement-marker fix; execution pending
M3f.3 manual report: page 1 accents and zoom/rotation/ordinary selection OK. Cross-item
AFIS/INCOSE selection still refused. Device diagnostic shows g0 U+FFFD at L5/L7 and other
item rows, not the U+000F assumed by the earlier reviewed font profile.
M3f.4 adds leading-only reliable U+FFFD normalization to generic bullet in exact known
AdvP4C4E74, preserving raw provenance and all geometry/column/accent rules. Other U+FFFD
remains refused. Source baseline 402 declarations; expected414 tests/twelve reader suites.
The native fixture now explicitly exercises ToUnicode->U+FFFD. Author runtime unavailable.
Read docs/M3f-4-ACCEPTANCE.md. No PDF mutation, history/save/recovery or cross-column claim.

## Historical status below (superseded)

# Current increment — M3f.3 supplied-document text repair; runtime acceptance pending
M3f.2 diagnostic-test repair accepted by user: actual 372 JVM tests/eleven reader suites,
build/Lint/install success. Remaining manual defects: dotless-i circumflex, false accent
word gaps and legacy C15 circle markers across entries. Supplied two-page PDF inspected.
M3f.3 adds bounded typographic dotless-i clusters, native synthetic-space provenance and
base-aware boundaries, plus reviewed AdvP4C4E74 U+000F-to-circle normalization in a copied
native walker. Expected 402 JVM tests/twelve suites, not executed by author. No blanket
control conversion, PDF edits, history/save/recovery or cross-column reading continuation.
Read docs/M3f-3-ACCEPTANCE.md and retest the supplied passages before declaring acceptance.

## Historical status below (superseded)

## M3f.2 current status — runtime acceptance pending
M3f.1 still fails the screenshot accent case and multi-item bullet selection by user report.
Standalone accents, not combining marks, appear in the supplied diagnostic rows. L5/L7/L9
have one rejectable glyph each but no unmapped glyphs; their exact symbols remain unknown.
M3f.2 adds geometry-checked adjacent spacing-accent quotation clusters and bounded
exceptional-symbol diagnostics. Unknown list markers remain explicit refusals.
Expected 372 JVM tests (348+24), eleven existing reader suites. No runtime pass claimed.
Read docs/M3f-2-ACCEPTANCE.md. No PDF mutations/save/history/recovery/column continuation.

## Historical status below (superseded)

# Current increment — M3f.1; runtime and same-passage acceptance pending

M3f user baseline: 328 JVM tests, eleven reader instrumentation checks and continuity
manual checks passed. Citation accents and some bullet rows remain open.

M3f.1 adds per-glyph fallback provenance, final-range validation, combining-safe
inferred boundaries, NFC automatic quotations, mapped black-circle list recognition
and content-free Unicode category counters.

Expected 348 JVM tests and the existing eleven reader instrumentation checks.
No runtime execution is claimed for this delivery. See M3f-1-ACCEPTANCE.md.
No PDF mutations, history/save/recovery or cross-column continuation added.

## Historical status below

# Current increment - M3f preview/tool continuity and citation structure; execution pending

- Accepted M3e user report: 298 JVM tests with zero failures/errors/skips, host/Lint/install
  successful, ten native suites PASS, all requested manual handle/ellipsis checks OK.
- M3f: start circle above text; completed current-page selection checkpoint survives
  pinch/cancel/ordinary rotation; page navigation retains chosen color but clears range;
  automatic citation preserves recognized list/paragraph boundaries while joining wraps.
- Expected JVM total 328 (298+30), eleven native suites. Author runtime execution unavailable.
- Checkpoint is memory-only copied current-snapshot data, not native annotation/recovery.
  New PDF/fresh process starts navigation. Semantic paragraph/list inference is conservative,
  not tag-tree or original source-line-break reconstruction. Ambiguous layouts remain.
- No native PDF writes, editing/history/save/recovery or cross-column reading-order claim.
  Read docs/M3f-ACCEPTANCE.md and await real rotation/tool/list/paragraph acceptance.

## Historical status below (superseded above)

# Current increment - M3e read-only endpoint handles; source supplied, execution pending

- User describes M3d preview as almost satisfactory after initializer build fix; ellipsis
  paint issue remains. Latest explicit successful automated log supplied was M3c.1. Require
  M3d applied receipt, corrected cached initializers and 268 source test declarations;
  do not invent an observed post-fix M3d test/native summary.
- M3e adds start/end handle routing, character-mode adjustment with exact fixed logical
  endpoint, and selected same-row dot/ellipsis paint-height assistance. No PDF mutations.
- APK/status M3e. Expected JVM 298 (268+30), ten native suites; author runtime execution
  unavailable. Existing flow/column, Unicode, physical-row and epoch safeguards remain.
- Read docs/M3e-ACCEPTANCE.md. Test actual handles/tap/slop/char quote/reverse/pinch/reset
  and punctuation. Screenshot alone cannot establish source ellipsis encoding/geometry.
- No cross-column reading-order, saved highlights, editable Contents/history/save/recovery
  or full accessibility/auto-scroll completion claimed. Native mutation durability remains.

## Historical status below (superseded above)

# Current increment - M3d responsive continuous preview; accepted with one known issue

- M3d manual tests (user report, 2026-10-25): working, except the "…" (ellipsis) character
  is not highlighted like other characters in the preview. Known issue, to be fixed in a
  later increment. Automated test counts/native suite results were not reported here.

- M3c.1 accepted user host/native report: 244 JVM tests without failures/errors/skips,
  build/Lint/install success, eight native suites PASS. Same-column physical-row fix works;
  diagnostics changed 123 -> 116 rows with physical glyph count preserved.
- Cross-column reading continuation remains unsupported. User reports delayed drag feedback
  and visible justified-word gaps in the temporary overlay.
- M3d adds immutable geometry caches, bounded weakly cached exact-hit indexing, current-
  gesture valid interim MOVE feedback, and UI-only continuous row paint bands.
- Expected JVM total 268 (244+24), nine native suites. Author runtime compilation/JUnit/
  Lint/PowerShell/Android execution unavailable. No measured speed/frame/heap promise.
- Selection glyph/quads/text, physical-row/column safeguards and final-result validation
  stay intact. No PDF writes, annotations/history/save/recovery or reading-order hop added.
- Run docs/M3d-ACCEPTANCE.md; user drag/gap/cancel/final-quote tests are required.

## Historical status below (superseded above)

# Current increment - M3c.1 physical text rows; source supplied, runtime acceptance pending

- M3c accepted automated user report: 216 JVM tests, zero failures/errors/skips,
  build/Lint/install successful and seven native suites PASS; visible version works.
- Real layout diagnostic exposes eight same-baseline fragments at y=282.86..290.18,
  with small word gaps, plus a paragraph gap exceeding the old cross-ID limit.
- M3c.1 coalesces supported same-block/run physical rows without synthetic glyph/quads;
  logical spaces are text-boundary metadata, preserved in quotation/word snapping.
  Native cleanup precedes normalization; cross-ID gap limit adjusts 0.9 -> 1.25 heights.
- APK/status version M3c.1. Expected 244 JVM tests (216+28), eight native suites.
  Author compiler/Gradle/JUnit/Lint/PowerShell/Android execution unavailable.
- Retest original fragmented row and paragraph boundary; do not declare universal
  paragraph/column/reading-order selection or native annotation/save/recovery complete.
- No UI layout/renderer/queues/workspace/build-dependency changes or original PDF writes.

## Historical status below (superseded above)

# Current increment - M3c diagnostics/version; source supplied, runtime acceptance pending

- M3b.1 targeted manual margins/short multiline/accents/zoom/column isolation improved,
  user report. Paragraph-related and some same-paragraph refusals remain unresolved.
- Last explicit automated log: M3b 182 tests/six suites PASS. Expected M3b.1 source
  baseline 206 tests/seven suites; latest full automated summary not supplied here.
- New M3c: visible status/APK versionName, accurate refusal labels, user-opened bounded
  geometry-only diagnostic report. No words/quotations/document identities in the report.
- Expected JVM total 216 (206 + 10 report-builder tests). Author runtime execution unavailable.
- Selection geometry, native extraction, rendering, queues, workspaces and column guards
  remain unchanged. No paragraph/cross-column reading-order fix is claimed by M3c.
- Read docs/M3c-ACCEPTANCE.md and share reviewed structural evidence for a failing case.
- Production selection completeness, handles, annotations/history/save/recovery remain
  pending. Do not erase known limitations or invent passed test/native results.

## Historical status below (superseded above)

# Current increment - M3b.1 selection flow/margin fix; execution pending

- M3b host/build/install and six instrumentation suites passed by user report: 182
  JVM tests, zero failures/errors/skips. Single-line, accents, colors/Hand, pinch
  cancellation and alignment/page switching passed manual checks.
- M3b manual acceptance remains OPEN: multiline and endpoints outside glyphs failed.
- M3b.1 changes domain selection only: text-line-band endpoint assistance and bounded
  geometric line continuation across native IDs/array ordering, with column safeguards.
- Expected 206 JVM tests (182 + 24) and seven instrumentation suites. Author compiler/
  Gradle/JUnit/Lint/PowerShell/Android execution unavailable. No observed pass claimed.
- UI/rendering/background queue/workspace/engine/build settings and PDFs untouched.
- Retest the SAME failed passages using docs/M3b-1-ACCEPTANCE.md. Do not advance real
  annotation/history/save/recovery until this acceptance is resolved.

## Historical status below (superseded above)

# Current increment - M3b interactive read-only selection preview; source supplied, execution pending

- Accepted M3a by user: 161 JVM tests, zero failures/errors/skips, five instrumentation
  suites PASS and ordinary reader working. Repository/commit created by user; no SHA claimed.
- M3b integrates serialized per-page snapshot extraction, off-UI coalesced pure selection,
  existing six left color slots as explicitly unsaved preview tools, glyph-quad overlays,
  native-origin-aware mapping and read-only quotation dialog. No native PDF annotations.
- Expected JVM total 182 (161 + 21 mapping/queue tests); six instrumentation suites.
  Author Kotlin/Gradle/JUnit/Lint/PowerShell/Android execution is unavailable.
- Reader navigation/rendering/workspace, palette order, note controls and 48dp toolbar
  layout are preserved. Hand exits preview; page/background/rotation clear ephemeral selection.
- Run docs/M3b-ACCEPTANCE.md. Save/history/notes/settings, endpoint handles and production
  annotation creation/recolor/delete/durability/recovery remain incomplete. No PDF writes.

## Historical status below (superseded above)

# Current accepted increment - M3a read-only structured-text/selection proof

- M3a was accepted by the user on 2026-10-21. The user reported 161 JVM tests with zero
  failures/errors/skips, five instrumentation PASS labels, and successful reader checks.
- This workspace's host build completed `:app:assembleDebug`, `:app:testDebugUnitTest`,
  and `:app:lintDebug` using Gradle 9.4.1/JDK 17.0.20.1. Gradle marked assemble and tests
  UP-TO-DATE and executed Lint. JUnit XML: 161 tests, 0 failures, 0 errors, 0 skipped.
  Lint HTML: 22 warnings and 3 hints.
- M3a supplies copied native glyph/quad snapshots and a conservative selection proof.
  It is not connected to production reader touch input.
- The accepted reader remains read-only. No PDF annotations are created or saved; editing,
  history, save/autosave, mutation recovery and M3b remain incomplete/not started.
- No tablet was installed or instrumented during this host validation. Teclast Android 10
  acceptance remains pending. See [M3a-ACCEPTANCE.md](M3a-ACCEPTANCE.md).

## Historical status below (superseded above)

# Current increment - M2c.4 bounded adaptive rendering; source supplied, execution pending

- Accepted M2c.3 user baseline: 114 JVM tests, zero failures/errors, build/Lint/install
  successful, clipping/recent storage/durable workspace PASS and manual checks OK.
- New source: bounded tiered whole-page refinement, native logical page dimensions,
  stable pixel replacement and separate cancellation/delivery tickets.
- Expected JVM total 136 (114 + 22). Author compilation/JUnit/Lint/PowerShell/Android
  execution is not available. See validation.json and docs/M2c-4-ACCEPTANCE.md.
- Gradle/VS Code settings, engine/dependencies/locks/wrapper, workspace retention,
  toolbar order, clipping and feedback timing remain unchanged. No PDF writes.
- Extreme zoom remains deliberately quality-capped. Heap/GPU/native memory behavior
  requires tablet testing; the bitmap payload limit is not a whole-process bound.
- Production editing/history/save/autosave/recovery/settings and wider validation
  remain pending. Wait for user device acceptance before advancing.

## Historical status below (superseded by this section)

# Current increment - M2c.3 durable baseline source delivered; execution pending

- M2c.2 accepted by user: build/Lint/install, 94 JVM tests, both instrumentation
  checks and all manual reader tests OK.
- M2c.3: verified synced app-private non-cache immutable baseline, source identity
  and byte fingerprint, partial-copy cleanup, guarded read-only retention,
  source-comparison helper and native-workspace proof.
- 20 new JVM tests; expected total 114. Author did not execute Kotlin/Gradle/JUnit,
  PowerShell/ADB or Android. See docs/M2c-3-ACCEPTANCE.md.
- Engine/dependencies/wrapper/locks, reader UI, geometry/gestures, properties,
  M2c.2 recents/location/retained ViewModel and original PDFs are not changed.
- This is not editable recovery, PDF saving or automatic conflict handling.
- After acceptance: bounded sharper rendering, then production text-selection work.
  Safe mutation/save/recovery architecture remains required; M2 is not declared complete.

## Historical status below (superseded above)

# Current increment - M2c.2 source delivered; execution pending

- Accepted M2c.1 baseline: user-reported build/Lint/install success,
  74 JVM tests, zero failures/errors, clipping PASS and device acceptance.
- New source: 50 recents, persisted full reading location, retained read-only
  native session, unavailable/storage errors, safe list removal.
- 20 new JVM tests; expected total 94, not an observed passed result.
- Author did not execute Kotlin/Gradle/JUnit/Lint/PowerShell/Android.
- Existing engine/dependencies/locks/wrapper/native proof/geometry/gestures,
  toolbar order, clipping, 1000ms feedback and properties are preserved.
- Run docs/M2c-2-ACCEPTANCE.md; wait for device results before advancing.
- Durable editable workspace, safe saving, sharper rendering, production
  annotation/history/recovery/settings and Teclast acceptance remain pending.
- Milestone 2 is not declared complete.

## Historical status below - superseded above

# Current increment — M2c.1 document properties

- User accepted M2b.4 reading continuity and one-second fade.
- M2c.1 adds a real filename-triggered properties dialog and serialized native metadata extraction.
- PDF creation/modification dates remain distinct from provider file modification time; unknown fields stay unknown.
- Added ten PDF-date parser tests. Known expected JVM total: 74. Author Kotlin/Gradle/JUnit/PowerShell/Android execution pending.
- Existing navigation state, clipping, feedback, engine/dependencies and annotation proofs remain unchanged.
- M2c.2 recents and restart-location persistence are next; retained native session/durable editable copy and sharper rendering remain pending.

## Earlier status (historical; superseded above)

# Current increment — M2b.4 reading continuity

- User accepted M2b.3 ripple removal. Previous confirmed baseline: 54 JVM tests, zero failures/errors.
- M2b.4 changes feedback to 1000ms and preserves zoom/proportional X on page navigation, resetting Y to top.
- Top-start and pending navigation proportion survive rotation; page jump shares the same behavior.
- Added ten geometry tests; known expected JVM total: 64. Source supplied, author execution pending.
- Clipping, icon layout, PDF engine, annotation proofs and document contents remain unchanged.
- Production editing/save/history/settings, recents/retained session and higher-resolution zoom remain pending.

## Earlier status (historical; superseded above)

# Current increment — M2b.3 toolbar feedback polish

- M2b.2 user results: 54 JVM tests, zero failures/errors; clipping runner passed; requested features reported OK.
- Remaining issue: standard circular Material ripple overlays the desired rectangular fade.
- M2b.3 disables the ripple locally inside PulseAction; existing 2000ms fade/actions remain unchanged.
- Source supplied; author has not run Kotlin/Gradle/JUnit/PowerShell/Android. Expected JVM count remains 54.
- No geometry, renderer, PDF, dependency or toolbar-layout changes.

## Earlier status (historical; superseded above)

# Current increment — M2b.2 navigation refinements

- User M2b.1 results: 41 JVM tests, no failures/errors; clipping instrumentation and reported reader checks passed.
- M2b.2 supplies bounded small-page horizontal pan, original SaveAs icon, page dialog, 2s feedback and centered navigation group.
- Source supplied, author execution pending. Expected JVM total: 54.
- Existing clipping, native engine and annotation/interoperability fixes remain unchanged.
- Editing/save/history/settings, recents/retained session and sharper rendering remain pending.

## Historical earlier status (superseded above)

# Current increment — M2b.1 revised bars and clipping

- Approved top center: Save/Undo/Redo/Save As. Open moves bottom left.
- Bottom: left Previous/Open/name; center Fit Page/number/Fit Width; right Hand/Settings/Next.
- Sixteen top targets preserve 48 dp widths. Colors unchanged.
- Screenshot-confirmed zoom overflow: add Compose and native Canvas clipping plus opaque bars.
- Added real width fit, 5 JVM tests, and synthetic device software-Canvas clipping test.
- Source supplied; Kotlin/Gradle/JUnit/PowerShell/Android execution pending. Expected JVM total: 41.
- Existing gestures, MuPDF and annotation/interoperability fixes remain unchanged.
- Editing/save/history/settings, recents/retained session and high-resolution zoom remain pending.

## Earlier status (historical; superseded above)

# Current increment — M2b compact reader layout

- M2a.1 latest device report: pan works from the page viewing area; header-origin touches do not pan; rotation passed.
- Supplied gesture trace shows positive and negative page movement. Prior isolated rotation freeze not reproduced, retained as a regression observation.
- M2b supplies compact approved tablet primary/footer layout and removes the diagnostic header.
- Only existing reader/navigation actions are enabled. Annotation/save/history/settings controls remain explicitly unavailable, without fake handlers.
- Source supplied; compilation/Lint/UI execution pending. Existing known test baseline is 36, no new tests in this layout increment.
- M2a.1 touch/geometry and M1.1 interoperability fixes remain unchanged.
- M2c recents/restart location/retained native session/working-copy persistence and high-resolution rendering remain pending.
- Tablet widths under 768 dp require layout approval; they receive an explicit warning, not silently compressed primary tools.

## Earlier status (historical; superseded above)

# Current increment — M2a.1 gesture hardening and diagnostics

- M2a host output: 29 JVM tests, 0 failures/errors; build/Lint/install succeeded (user report).
- Device: pinch/Fit/pointer lift passed; fresh downward pan unreliable; one post-rotation pan/zoom freeze.
- Root cause is not established. M2a device acceptance remains OPEN.
- M2a.1 targets stale gesture transitions and adds filtered, debug-only event tracing plus 7 JVM tracker tests.
- Source supplied; host/device execution pending. Expected known JVM count: 36.
- No engine, dependency, renderer, coordinate-math, native proof or original-document changes.
- Retest and collect the trace as described in docs/M2a-1-ACCEPTANCE.md before advancing.

## Earlier status (historical; superseded above)

# Current increment — M2a viewport foundation

- M1.1 fixture interoperability gate: passed; native/independent PDF checks plus Docear/Obsidian extraction (user reports).
- M2a adds real read-only pinch/pan/fit and rotation viewport restoration; source supplied, host/device execution pending.
- Seven new JVM geometry tests; expected known-baseline total: 29.
- Approved final toolbars, recents and persistent restart location remain M2b work.
- Rendering resolution remains the smoke bitmap; high-zoom rerendering/cache is not yet implemented.
- Existing Activity-owned session reopens on rotation; retained native session and durable editable copy are pending.
- Editing, complete history, save/autosave/recovery, Teclast validation and final accessibility remain pending.
- M1.1 files, engine and dependency versions are unchanged. See docs/M2a-ACCEPTANCE.md.

## Earlier status (historical; superseded above)

# Current status — M1.1 quotation normalization and metadata update

- Previous native proof: passed on Lenovo API 30 (user device output and supplied report).
- Independent inspection of supplied original stages: selected structural checks passed; raw private text hints had an encoding inconsistency.
- M1.1 supplied source: automatic line-wrap normalization, explicit paragraph groups, manual-newline preservation and marked UTF-16BE private text strings.
- New tests: 15 JVM tests; expected total with the known 7-test baseline is 22.
- M1.1 has NOT been compiled, run through JUnit/Lint or executed on Android by the author.
- General paragraph/column inference is NOT implemented; this fixture explicitly declares paragraph groups.
- Build/dependency/wrapper/lock versions stay unchanged. Both prior native-call workarounds and full error reporting remain.
- Final reader UI, complete undo/redo, autosave, recovery and Teclast validation remain pending.
- Actual Docear/Obsidian extraction is still required before the full reader interface.

## Earlier initial status (historical; superseded above)

# Project status
Current increment: M0.1 read-only open/render source bundle.
Milestone 0 is NOT passed. Milestone 1 has NOT started.

Implemented source (not compiled here):
- minSdk 29 and stable application identity with .debug suffix.
- French Compose/Material 3 smoke screen.
- System PDF picker with only granted read permissions persisted when offered.
- Read-only local working copy, MuPDF page rendering and page navigation.
- Permission-aware text-layer character count (no quotation logs).
- Serialized native cleanup and stale-result suppression.
- Explicit password/native-load/open failures.
- Rotation instance-state URI/page restoration.
- Available-width/API/ABI diagnostics.
- Editable-highlight-text policy model and seven JUnit tests (not run).
- Windows official-wrapper bootstrap and MuPDF AAR inspection scripts (not run).

Executed here:
- Tool inventory: java, gradle, adb, sdkmanager, kotlinc, pwsh all absent.
- Exact tagged Java API source inspection.
- Python generation, XML/resource checks, ZIP/hash checks and two-page fixture
  accent extraction using installed pypdf; recorded in validation-report.json.
  pypdf is a build-environment verifier, NOT a second application PDF engine.

Not executed:
- Gradle dependency resolution, wrapper bootstrap, AAR probe, Kotlin compilation,
  JUnit, lint, APK build/install, native loading, tablet tests, downstream tests.

Blockers:
- Exact AAR availability and actual native ABIs.
- Complete candidate-stack resolution and effective Kotlin/compiler alignment.
- Actual tablet portrait width versus one-row target budget.
- Missing generated official wrapper/lockfiles until Windows bootstrap/build runs.
- Third-party license/notices audit before distribution.

Next: run README steps, obtain host/device evidence, fix integration failures,
then deliver the native annotation interoperability proof. Do not build the full
annotation UI before its gate. No external repository has been modified.
