# Active automation repair — M4a-R5; acceptance pending

## M4a-R5 — comment-tap frame synchronization; focused then full acceptance pending

Observed R4:874 JVM PASS,30 native PASS,89/90 workflows PASS; all margin and fallback
cases PASS. Only comment-second-tap-apply-and-history timed out at first-tap selection.
It passed in R3; this is not proof of an app defect or confirmed sole cause. Keep R4.
R5 changes only automation helpers/focus metadata: wait for the current drawn logical
frame, bitmap/text/preview/tool binding and focused visible view; calculate a unique
highlight glyph target and coordinates in one UI-thread snapshot; send ONE physical tap.
Never set selectedHighlight or open/apply an editor directly. No retry tap, blind delay,
post-selection assertion waiver or deadline increase (first tap shares20s preparation/result).
Retain R1 create synchronization/R2 MEGA/R3 import/R4 real pan driver and all existing tests.
First run only the WHOLE comment second-tap/apply/native-save/Undo/Redo workflow; require
its exact structured PASS before launching the unchanged full874/30/90 install gate.
Focused PASS is NOT full acceptance. Record numeric/boolean comment-frame diagnostics.
No app/debug source change and no author platform pass is claimed. Last confirmed working
installation remains M3z until a matching actual M4a FULL/install receipt establishes otherwise.
## Earlier source/repair history

# Active automation repair — M4a-R4; full acceptance pending

## M4a-R4 — physical pan-driver threshold repair; platform acceptance pending

Observed M4a-R3 FULL:874 JVM PASS, all30 native suites PASS,88/90 workflows PASS; both
failures occurred in marginEdge while establishing the centred edge. All4 new fallback
workflows passed. Working installation remained blocked. Preserve that report and R3 source.
The old driver restarted a gesture for each residual correction; a2px-to-touchSlop residual
could repeatedly be treated as a tap. It also failed without measuring its twentieth swipe.
R4 changes ONLY marginEdge in the automation kit, not app/debug source. It crosses the
actual device threshold and corrects small overshoot while the same finger remains down.
Maximum20 gestures/eight MOVE samples/24ms cadence and strict2px target stay unchanged.
No setter/reflection state change, synthetic mutation, timeout increase or assertion waiver.
The final swipe is measured, and failure records numeric geometry/slop diagnostics only.
Model reproduction supports this repair but is not device proof or the confirmed sole cause.
Full874 JVM/30 native/90 workflows remains mandatory before working install-r. All previous
R1 rendered-frame guards/R2 MEGA policy/R3 import/native/private-reference assertions remain.
## Earlier source/repair status

# Active repair — M4a-R3; acceptance pending

## M4a-R3 — debug proof import repair,2026-11-15

M4a source is applied; isolated FULL run20261115-193044-c5bc7f4a00a64a6b966c9bff0eaf0411
FAILED during compileDebugKotlin: AuthorizedFallbackInstrumentation lacked the import for
existing domain.CheckpointFingerprint. JVM results were null; native/workflow tests did
not run. Working APK was not replaced; last confirmed installed app remains M3z.
R3 adds only that import to the debug proof; production application sources/behavior and
all assertions remain unchanged. Preserve R2 MEGA exclusions and old packages/reports.
Expected874 JVM/30 native/90 workflows remain mandatory; no author runtime pass is claimed.
Use the new matching FULL/install receipt for subsequent actual status, not this pending note.
## Earlier M4a source-delivery status

# Current status —2026-11-15 / M4a source delivery

Accepted/installed baseline:M3z on Lenovo/API30,846 JVM/29 native/86 workflows all PASS.
QA baseline:M3z-R1-078d9248c79ebf3b. Historical pending-status stack is archived under
history/STATUS-through-M3z.md; it is not the current project state.

M4a source adds optional authorized automatic fallback after eligible Source Save failure,
with local-time names/collision suffixes, durable RW tree authorization, inventory/source-
alias/nonempty checks, verified readback, preserved local/dirty/source/navigation state,
and explicit Settings/Help. It also preserves existing native highlight opacity on edits.
No timer, networking, dependency change, source rebind, recovery cleanup or data clear.

Expected874 JVM/30 native/90 workflows, all PASS before working install-r. Author runtime
execution unavailable. Adds28 JVM,1 synthetic native suite and4 synthetic-gateway UI/fault
cases. All846/29/86 retained. Real system-picker/provider behavior remains a manual check.

Whole-project result: see PROJECT-RECONCILIATION.md/.csv. Two substantial gaps before this
delivery were fallback and compatible imported editing; existing-opacity handling was an
additional preservation defect now fixed in source. Imported editing remains final functional
work, then one consolidated production PDF/provider/downstream/device/release signoff.
B/C were satisfied by the user's later recovery/warning decision. Initial Docear/Obsidian
fixture proof passed; do not reopen it as never tested. No universal release claim is made.

## Binding MEGA sync-conflict policy — M4a-R2

Files whose leaf name ends with `(conflict yyyy-MM-dd-HH-mm-ss)` before an optional
extension are user-authorized MEGA sync artifacts, not canonical project sources.
Exclude them from source baselines/guards, QA inventory/resources and future AI exports.
Use the canonical filename (especially AndroidManifest.xml). Archive such source copies
outside the active project before builds so Gradle cannot compile duplicate Kotlin/XML.
Never merge them into the app or disable guards for other files. User authorized removing
these copies; R2 keeps local recoverable quarantine rather than permanently deleting them.
AGENTS documentation has a verified before-backup; unrelated source mismatches still block.
