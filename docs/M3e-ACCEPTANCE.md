# M3e - preview endpoint handles and punctuation painting

## Baseline, evidence and scope
User says M3d is almost satisfactory after the initializer build fix, with an ellipsis
paint issue in a screenshot. Latest complete supplied successful automated log was M3c.1
(244 JVM tests/eight suites); the later M3d post-fix log was not supplied. Do not fabricate
an observed 268-test result: require the applied M3d receipt, hashes, build-fix markers and
268 source test declarations, then establish runtime results from the user's run.

M3e is the next substantive preview increment, NOT a punctuation-only patch:
- draggable start/end handles after a completed valid selection;
- character/combining-safe range adjustment, exact fixed logical endpoint;
- low-height dot/ellipsis UI-paint assistance using selected same-row context;
- version M3e in APK/status, unchanged permanent toolbar structure.
No PDF annotation, dirty revision, Save/Undo/notes, recovery or column continuation added.
All preview text remains UNSAVED and is discarded under the existing page/lifecycle rules.

## How to use
Choose a color and make a normal word-snapped selection. Once it completes, two colored
circles/stems appear at its boundaries. Drag either circle to adjust by actual characters,
not full words; the opposite logical address remains fixed. Touch targets use a 24dp radius
(48dp diameter); the visual circle is smaller. Near a visible handle it takes touch priority.
To start an unrelated selection, begin away from handles or clear via Hand/active color.

A tap without movement keeps the selection unchanged. Movement must cross platform touch
slop first; after that it follows character positions. The marker follows the finger during
adjustment and settles at the final sorted glyph boundary after release. Moving across the
fixed endpoint can reverse the range. Inferred spaces have no independent glyph geometry;
combining sequences/ligatures cannot be subdivided into fictitious characters.

The fixed endpoint is a TextAddress, not a re-hit-tested visual point, so opposite combining
marks/zero-area punctuation are not silently dropped. Moving endpoints still use the tested
sheet/line-band resolver and current column/flow guards. A rejected final adjustment clears
or refuses the preview; no partial PDF edit or hidden draft is saved.

Pinch/second finger, ACTION_CANCEL, page/PDF/mode changes, background and rotation cancel
active handle adjustment and invalidate pending work. Existing gesture epochs/snapshot/
release gates stay. No auto-scroll, off-screen endpoint navigation, stylus-specific handling,
virtual TalkBack handle nodes or cross-page/cross-column reading-order mode is claimed.
Handles whose text anchor is far outside the viewport are hidden; near-edge circles clamp
inside the Canvas clip. Fit Page can help keep endpoints visible. No drawing over toolbars.

Citation shows the newly adjusted read-only quotation, preserving manual-Contents policy:
there is no editable/native Contents draft in this preview increment. Save/history remain
disabled until mutations have completed-edit persistence safeguards.

## Ellipsis handling and limits
The screenshot shows a thin/unfilled ellipsis region but cannot prove its source Unicode,
font geometry or whether it is included in the selected glyph list. Paint-only handling
recognizes U+2026 and ASCII periods. If selected neighboring text confirms a same-row band,
a low-height/zero-height but nonzero-width dot borrows that UI paint height. Limited forward
lookahead handles a selection beginning with punctuation. No text character, glyph quad,
quotation, gap metadata or PDF object is changed or invented. Isolated punctuation with no
reliable context uses its actual geometry; zero-width/missing glyphs are not fabricated.
If a gap remains, confirm whether the punctuation is actually present in Citation and share
a reviewed structural report; do not assume a screenshot establishes the extraction cause.

## Ownership, threading and safety
Native snapshot extraction/cleanup and physical-row coalescing remain unchanged and serialized.
Handle coordinates use the same native-origin-aware PageCoordinateMap as preview bands.
View captures the fixed original address and grab offset, avoiding a jump when touching a
circle below the text. Pure adjustment is dispatched off the UI thread through the same
one-running/one-latest-pending queue, with explicit word/character intent and fixed address.
Current-epoch valid progress is permitted; final results remain latest-only. Real glyph/quads
stay separate from paint bands and handle circles; never export UI rectangles as QuadPoints.
No native objects enter UI state; no source quotations/document identity are logged.

## Delivery/install
Extract separately. Run Apply-M3e.ps1 -ProjectRoot ... -BuildAndInstall. -ValidateOnly changes
nothing. After applying once retry Validate-M3e.ps1, not Apply. -AdbPath overrides discovery.
No SDK/Gradle/JDK/lock/dependency/editor/Git changes, original PDF writes, uninstall/data clear.
Use the working Windows session. Do not rerun older SDK/XML/application repair scripts.

The updater verifies ALL 14 source hashes in the applied M3d receipt (including the corrected
initializer source), plus known safety files and 268 test declarations. New source is hashed;
existing code uses reviewed exact-one-match patches preserving unrelated text and line endings.
The complete paint helper is replaced only if its current hash matches that recorded M3d source.
Any local source drift stops. Docs/status/decisions/agents/requirements are merged. Backups
and created-file records precede writes; write-phase failures attempt targeted rollback.
Later build/device failure leaves source applied. New .validation/M3e-applied.json receipt
captures old protected sources plus transformed/new app source hashes for guarded retries.

Expected JVM total: 298 = prior source baseline 268 + 30 new tests.
Validator builds/tests/Lints, installs with -r, reruns nine earlier suites and adds:
  PASS: endpoint handles
The synthetic native-view test checks handle tap no-op, slop/grab-offset routing, character
rather than word range, exact opposite endpoint, release and ACTION_CANCEL. It is NOT real
Compose/multitouch/rotation/accessibility/performance validation. Existing native extraction,
physical-row, paint and epoch tests also rerun. Source counts are expectations, not passed logs.

## Six tablet checks
1. Confirm M3e. Select a sentence normally. Start/end circles appear; touching and releasing
   without dragging must not change it. Toolbar order/targets/status height remain unchanged.
2. Move the end circle inside a word, then the start circle inside a word. Citation must
   reflect character-level changes while keeping the opposite endpoint. French accents,
   combining characters and inferred word spaces should remain intact. A ligature is one
   native glyph and cannot offer fake internal geometry.
3. Move one endpoint to an adjacent permitted line and try reverse crossing. Markers should
   settle at sorted boundaries after release. Column/gap refusals remain intentionally
   conservative; verify no stale quotation or canceled range reappears.
4. During handle drag, add a finger and pinch; also test Hand/color exit, Fit/refinement,
   page/PDF changes and rotation. Adjustments cancel safely; no annotation is saved.
5. Retest both U+2026 and three-period examples where selected ordinary text exists beside
   them. Internal ellipsis paint should share row height. Check Citation still contains
   exactly the original punctuation. No gutter or vertical paragraph rectangle is filled.
6. Repeat on the previously troublesome justified physical row. Speed/continuous feedback,
   word boundaries, glyph alignment, diagnostic and normal new selections must stay usable.

Return build/JVM/ten-native summary and failed checks. No private PDF upload needed.

## Evidence / next gate
Author lacks Java/Javac/Kotlin/Gradle/PowerShell/Android SDK/ADB. No compilation/JUnit/Lint/
PowerShell/native/tablet pass is claimed. validation.json records static/reference checks.
298 is expected until observed on the user's host. After handle acceptance, verified column
reading-order continuation remains a separate increment before complete editing claims;
real highlights/history/saving require completed-edit durability, not just good previews.

Public platform contracts checked:
https://developer.android.com/reference/android/graphics/Canvas
https://developer.android.com/reference/android/view/MotionEvent
https://developer.android.com/develop/ui/views/touch-and-input/gestures/multi
