# M3c - structural selection diagnostics and visible version

## Current acceptance: precise scope
User reports M3b.1 margin endpoints, ordinary two/three-line selection, quotation/accents,
zoom and column isolation improved. Some same-paragraph/new-paragraph selections still
refuse depending on PDF layout. Bottom-of-column-1 to top-of-column-2 reading continuation
is not implemented and stays refused. These are not final-product completion claims.
The last supplied explicit automated log was M3b: 182 tests, six native suites PASS.
The M3b.1 source baseline adds 24 tests and a seventh suite (expected 206); its later full
automated summary was not supplied in the latest message. Do not invent an observed count.

M3c is an explicit diagnostic development increment, not a selection-flow fix or finished
annotation release. It does not increase spacing thresholds, weaken column guards, enable
PDF mutations, or promise universal paragraph reading order. The outstanding layout cases
must be investigated using structural evidence rather than arbitrary tolerance increases.

## New visible behavior
- APK versionName is M3c; debug versionCode, package ID, build stack and dependencies stay.
- The existing single-line status strip starts with M3c in all reader states. Its height
  and the permanent toolbar arrangement/48dp targets are unchanged.
- Refusal wording no longer misleadingly claims that every range must stay in one native
  block. Distinct messages identify the selector's DECLARED refusal enum; they are not
  a diagnosis of the exact failed geometric predicate or semantic paragraph structure.
- When a selection is refused (or native text analysis fails), a contextual Diagnostic
  button appears in the same viewport overlay area used by Citation. No page resize.
- Diagnostic is also available from the read-only quotation dialog for a successful
  nearby single-line selection, useful for comparing a failing layout to a working one.
- The report is a frozen, bounded structural DTO: version, snapshot status/rejection enum,
  native page bounds, total line/glyph counts and up to 200 raw-order line entries with
  index/block/run/bounds/glyph count/compatibility. Builder maximum is 500 rows.
- No words, code points, quotations, document name/URI/author or document bytes enter the
  report. It contains layout structure, so still review it before sharing. No report is
  automatically logged, written to disk, transmitted or copied to the clipboard.
- Report text is selectable for an intentional copy. Closing/rotation/page change drops
  the report; there is no edited draft or private document recovery state to lose.

The increment code appears in two metadata locations (APK versionName and reader_increment
resource). This package generates/checks both as M3c. Future application-source increments
must update both and the matching documentation; don't leave an old status version behind.
This is development milestone metadata, not a production release/versionCode strategy.

## Why the remaining behavior is not simply relaxed
M3b.1 currently limits continuation by native geometry: compatible font/line heights,
horizontal alignment/overlap, bounded vertical gap and unambiguous next-row choices.
Different native IDs are allowed but use tighter gap limits. A normal paragraph can still
be refused if extraction fragments/order/metrics defeat those rules: that is a remaining
correctness/coverage issue, not user error. Real source structure has not been inspected.

A new paragraph on the same column and a hop to the next column are separate problems.
Cross-column reading continuation needs an explicit verified reading-order model so a
short diagonal drag never silently merges unrelated columns. M3c does not implement it.
General Unicode/grapheme/layout/OCR capabilities and all M3a/b limitations remain.

## Apply/build/install
Extract separately and run Apply-M3c.ps1 -ProjectRoot ... -BuildAndInstall. -ValidateOnly
runs preflight with no edits/build. After applying source once, retry Validate-M3c.ps1,
not Apply. -AdbPath overrides discovery. No admin rights, upgrades, --write-locks, Git
operations, uninstall, data clear or original PDF writes. Your commit remains unchanged.
Commit your current M3b/M3b.1 work separately first if you want clean increment history.
The ZIP's commit-message-M3b.1.txt describes that existing preview baseline, not M3c.

Exact hashes protect the changed ReaderScreen (as delivered in M3b; M3b.1 left it intact)
and current module Gradle file; source changes made during commit preparation stop safely.
Existing safety files/markers and the 206 pre-update test declarations are checked.
New-file collisions stop. Docs/status/decisions/agents/requirements are merged.
Backups/created-file records are made before writes. Write failures attempt rollback;
a later build/device failure leaves source applied for diagnosis. Never bypass guards.

Expected JVM total: 216 = prior source baseline 206 + 10 diagnostic-builder tests.
Validator builds/tests/Lints, checks JUnit XML, installs with -r, reruns the seven existing
instrumentation suites and launches the reader. No new native feature/suite is claimed:
new report behavior is pure data + Compose and requires the manual UI checks below.

## Four checks
1. Fresh launch, open a PDF, select text and cause one refusal. Status must show M3c in
   each case. Android app metadata versionName is M3c too; no reinstallation/data clear.
2. Cause the SAME paragraph refusal reported previously. Tap Diagnostic, inspect the
   report, long-press/select/copy only if you choose, and verify no words/quotation/name/
   URI appear. Close it. Document text must not be modified or exported automatically.
3. Select a nearby working single line, open Citation, then Diagnostic. The quotation
   remains in its separate read-only dialog, never added to the structural report.
4. Recheck Hand/color exit, pinch cancel, Fit/zoom alignment, page switch/rotation and
   recents/properties. No new selection behavior is promised in this increment; known
   refusals remain. Toolbar placement/touch targets/status height must stay usable.

## Input needed to address the document-dependent cases
After reviewing, share the structural diagnostic text for one reproducible failure and
one nearby working single-line case on that same page, plus:
- ordinary prose vs columns/table/formula;
- approximate location (top/middle/bottom), number of intended lines and zoom;
- whether the endpoints were on glyphs or line margins;
- whether you intended same-column paragraph continuation or a true column transition.
A redacted screenshot showing only the relevant layout can help map native row indices;
no private PDF or full quotation is requested. This report does not automatically capture
last pointer endpoints or prove the exact rejection cause; do not overinterpret it.

## Evidence boundary / next gate
Author runtime has no Java/Javac/Kotlin/Gradle/PowerShell/Android SDK/ADB. Compilation,
JUnit, Lint, native/UI/device results are NOT executed here. validation.json records
static/package/reference checks, not runtime acceptance. 216 is expected, not passed.
Once structural evidence is available, refine paragraph/column models and regression
fixtures before claiming full selection coverage or enabling native annotations/history/
save/recovery. Endpoint handles remain future work; no automatic Git commit/push is run.
