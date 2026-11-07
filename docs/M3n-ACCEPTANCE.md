# M3n acceptance — application update, runtime results pending

Baseline: actual217-file M3m export from2026-11-07; user baseline630 JVM/21 native
suites passed. This update expects654 JVM tests/22 native suites. No engine/dependency/SDK change.
24 new JVM cases cover saved-content comparison, lens geometry and recovered-target binding;
new View native suite checks same-page edit/refinement geometry and lens up/cancel/draw.
Existing630 test declarations and21 native suites are preserved.

## Implemented
- Recovered/previously failed source Save is enabled. After write authorization, read/validate
  a fresh external snapshot and show actual target name plus explicit overwrite warning.
  Approval binds that snapshot hash; another change before write is rejected. The existing
  verified before/after backups/intent/readback protocol is reused. No silent merge or rollback.
  An identical-byte no-op recovered Save still binds the original owner WITHOUT provider output.
  Signed/encrypted/unreadable/unwritable targets still need Save As. This is not provider CAS,
  atomic replacement, cloud completion or hardware-power-loss protection. Backups retained.
- Open/recent navigation offers Save/Continue without saving/Cancel for unsaved content.
  Continue NEVER deletes checkpoints. Save failure/authorization cancel never opens the pending
  document. Save As counts as externally preserved content in the current session, without
  switching source URI; after restart, original divergence can conservatively prompt again.
  Compare content hashes rather than revision counters; Undo to saved bytes is clean.
  Pending prompts/actions live in the retained ViewModel across rotation, not across process
  death; restoring an Activity result does not authorize an automatic source write.
- Same-page annotation revision and page identity are separate; edit/refinement pixels no
  longer cause page-fit/reset. Keep logical geometry unchanged and preserve frame diagnostics.
  Editing no longer inserts/removes the document-loading progress strip or preview mode.
  Status row has fixed height/no fleeting busy text; export messages overlay rather than resize
  the PDF. Native view remains input-disabled during durable edits. Error visibility retained.
- Canvas text lens,1.8x, above the moved text endpoint; below/clamped when space requires.
  Renders the existing bitmap/selection only, no handles/self recursion, bitmap allocation,
  new native raster or IO per motion. Lens sharpness is limited by currently available pixels.
  Clears on up/cancel/pinch/focus loss/page/size changes/disposal. No source-page zoom change.
- Keep verified per-completed-edit commits; no batching away the last2–3 edits.
- Guard nullable parentFile without!!; retain strict intent path/identity checks.

## Manual tests — disposable local Documents PDF FIRST
The following checks are pending after the automated validator succeeds.

###1 Existing workflow
Create/select/recolor/resize/delete in all six colors. Same-color deletes; Hand deselects.
Undo/Redo; accents/ellipsis/reversed/multiline/two-column/inline-reference selection. No new
annotation duplicates, darker completed fill, missing dots or changed third-party annotations.
Correct Citation manually, resize/Undo/Redo/Save/reopen; exact Contents stays corrected.

###2 Stable viewport / no blink
At Fit Page, Fit Width and arbitrary zoom+pan, repeat create/recolor/resize/delete/Undo/Redo.
Watch first edit raster AND sharp refinement: no movement/fit reset, zoom nudge or progress
strip/status-row blink. Repeat20 edits to detect cumulative drift. Save then edit again:
removal of Save success message must not resize PDF. Rotate/pan/pinch and retest. Actual
rotation may reconfigure view for new screen size, but normal edit/refinement must not.

###3 Ordinary Save / persistent local work
Save A; inspect source in another reader. Add B without Save, force-stop/reopen same URI:
recover A+B locally while source remains A. Explicit Save, inspect A+B externally. Repeat Save
without edits: no duplicate annotations. Undo back to saved content, Redo and Save; verify
external bytes/annotations. Save As stays a separate target and never silently rebinds source.

###4 External conflict / recovered overwrite
Save A, add B unsaved, externally change disposable source to C. Reopen source shows C and
recovery notice. Recover local A+B. Save: actual current source name and explicit warning
that C will be replaced. Cancel: both C and local A+B intact. Confirm: external source A+B,
verified backup of C retained; local head intact. Add D without Save and reopen: local A+B+D
must recover under original owner binding. Also test source already identical to recovered
copy: Save is a no-rewrite owner binding, subsequent unsaved edits remain recoverable.

###5 Source changes during confirmation / provider failures
After recovered Save confirmation appears, change source again externally. Confirm old
approval: conflict refusal, no overwrite, local head retained. Retry must obtain a fresh
snapshot/confirmation. Wrong authorization URI/cancel/read-only/missing/unreadable/protected
source: no unintended write; Save As remains. Fault injection covers partial write/readback;
do not intentionally interrupt a valuable original. No blind rollback after an unknown write.

###6 Unsaved navigation — all branches
After local edits, Open picker AND a recent document: correct current-name prompt.
Save: external success before switch. Continue: switch, return/recover, every committed local
edit remains. Cancel: same page/viewport/document. Cancel target picker: remain current.
Failed Save/cancelled authorization/cancelled overwrite approval: no pending switch.
Clean source/Undo to exact saved bytes: no unnecessary prompt. After successful Save As in
same session, no redundant unsaved-content prompt; original URI still unchanged.

###7 Dialog lifecycle
Rotate during navigation prompt, source authorization, replacement confirmation and Save:
no repeated picker, double Save or wrong target. Background/resume retains correct state.
Force-stop after local commit: recovery remains; do NOT auto-resume a pending overwrite/open
from an old Activity result after process death.

###8 Lens — both handles
Drag start/end individually: enlarged text follows endpoint glyph, not circle/finger offset.
Test page top/bottom/left/right, column boundaries, zoom/pan, portrait/landscape, reversed
selection and crossing handles. Window stays within viewport; no recursive lens or page zoom
change. Enlarged text corresponds to actual selection. Tap without drag/pinch/Hand/pan has
no persistent lens. Release/cancel/second finger/focus loss/rotation/page change all dismiss it.
Rapid drag and drag immediately after an edit must not enqueue native renders or disk writes
per move. A temporary soft bitmap can produce a soft lens until refinement; no fake sharpness.

###9 Platforms / regression evidence
Lenovo and Android10 separately; local Documents first, optional cloud/SD provider later on
DISPOSABLE files. Preserve existing native/app data; never uninstall/clear data to resolve
validation. Review warnings: previous strip/cast warnings may remain, nullable-parent warning
should disappear. Compare edit timing/visual stability; onDraw is not physical presentation.

## Automated / packaging gates
Apply source hashes against actual M3m snapshot; verified complete-file payload/backup/rollback;
preflight scans every main values XML for duplicate string IDs, independent by qualifier and
product, using XPath/GetAttribute (not XmlElement.Name). No automatic removal of unknown files.
Build/JVM/Lint receipt; DeviceOnly accepts only matching source/APK/reports, then install22
native suites. Apply has local create/replace/restore self-test. No empty-string Replace or
path escaping patches; generated PowerShell contains no String.Replace calls.

## Evidence boundaries
Author lacks Java/Kotlin/Gradle/PowerShell/ADB runtime. Source/resource/lexical/reference/ZIP
checks here are NOT a successful compile, JUnit/PowerShell execution or device run. The new
native suite is a View/Canvas synthetic check, NOT full Compose dialog automation or physical
hardware lens testing. All manual checks above remain acceptance gates. Do not infer generic
SAF atomicity, all-provider compatibility, a full recovery-version chooser or retention UI.
