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
