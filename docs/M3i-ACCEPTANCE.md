# M3i — first interactive durable LOCAL native highlights

## Accepted baseline
User report M3h: build/Lint498 JVM tests0 failures/errors/skips, all16 reader suites PASS,
install/launch success, no observed reader regression. No further M3h manual check required.
Selection, geometry, accents, color palette/order and native ownership rules are frozen.

## New visible workflow
Choose one of the six existing left color tools, drag/select text, adjust its handles and
inspect Citation if needed. The existing floating Citation surface now also offers Valider.
Valider creates an actual native PDF Highlight in an app-private checkpoint copy. It clears
the temporary range after durable commit and refreshes the same page without resetting the
viewport. The source/provider PDF is NEVER opened for output. This is not source Save.
The primary Save/Undo/Redo/SaveAs tool positions and48dp slots stay unchanged; Undo/Redo are
now real operations for highlights CREATED DURING THE CURRENT OPEN DOCUMENT SESSION.
Save/SaveAs, notes, settings, existing-highlight tap/recolor/delete and edited Contents UI
remain unavailable; those controls are not given fake handlers. No automatic commit on drag
release is added yet: explicit Valider makes the first durable creation boundary unambiguous.

## Persistence, identity and limits
- Separate noBackupFilesDir/reader-highlight-checkpoints-v1 namespace; exact URI hash plus
  source-byte fingerprint identify the working generation. Reopening the same URI/unchanged
  source recovers the current validated M3h checkpoint and displays native annotations.
- Changing documents/restarting clears the in-memory creation Undo/Redo history, NOT the
  committed annotations. Current-session history is bounded by real geometry+text units
  (roughly50k, retaining at least the latest supported command). This is NOT full persistent
  annotation history or final content-based dirty-state implementation. Revision existence
  is not presented as a dirty flag: undoing everything may still leave local revision records.
- Original source changes are not silently merged/adopted. Another generation or unverified
  local state keeps all data, shows a warning and disables new edits until future recovery
  management. Ordinary reading can display the original if a local recovery fails.
- THIS FIRST UI INTEGRATION STILL REQUIRES THE ORIGINAL SOURCE TO BE AVAILABLE FOR REOPEN.
  If its URI/provider becomes unavailable, local checkpoints are preserved but a standalone
  recovery chooser/offline-source fallback is not implemented. Do not uninstall/Clear data.
- External Save/SaveAs is not enabled yet. These annotations live in a real native PDF but
  INSIDE the app, not in the original and not yet exportable through this UI. Use test PDFs
  for first acceptance; do not treat this alpha as the sole copy of irreplaceable notes.
- Only unencrypted, unsigned PDFs with copy+annotation permission are offered Valider.
  Mutation uses a separate borrowed native candidate, closes all Page/Annotation objects,
  syncs/validates/commits before replacing the live reader. Structural page changes are not
  introduced. Signed/encrypted and unknown recovery states remain readable where supported.
- Real selected quads are used, not UI paint rectangles. RGB palette is unchanged and opacity
  follows the existing provisional0.5 native proof. Marker/text source normalization remains.
- Metadata NM uses a unique highlight UUID. Automatic Contents plus SGCHTextMode/Quotation/
  SyncedContents use the accepted UTF-16BE private-string protocol, ready for later manual
  Contents editing. Imported/other annotations are never selected/deleted by these commands.
- Creation retry retains the same command UUID for an unchanged pending preview. An uncertain
  publication is resolved by its operation ID; no blind duplicate replay. Undo/Redo mutations
  use fresh checkpoint IDs and only the exact owned Highlight NM on its recorded page.
- Busy editing disables interaction/navigation. Ordinary rotation retains the ViewModel;
  completed private commits survive process death. If post-commit refresh fails, report that
  the revision is kept rather than claiming rollback; page reload/reopen retries display.
- Native work/extraction/render/disposal stay on the existing serialized owner. Published
  bitmaps are not immediately recycled. History/current handles contain copied DTOs only.
- M3h full-file checkpoint IO/hash costs still apply; no frame/latency or power-loss claim.
  Old private checkpoints are maintained only after swapping/closing the old native reader.

## New verification code
20 JVM methods cover source identity, no URI leakage in directory names, history ordering,
redo branching/idempotent acknowledgement, resource budgets, all six colors, command IDs,
geometry/text validity and session-history reset. Existing498 unchanged -> expected518.
New17th suite PASS: local highlight workflow uses a self-authored PDF, real selected native
quads and the production backend to add two highlights, undo/redo, recover via a fresh
workspace and verify count/Contents/quads and unchanged original bytes. In-memory history
is explicitly empty on recovery. It is not a Compose touch, system-picker or real process-
kill test; those are manual. Existing16 suites are retained unchanged.

## Install/retry
Apply-M3i.ps1 -ProjectRoot ... -BuildAndInstall -DeviceSerial HA1B2KKW.
Verify complete M3h receipt/inventory and498 declarations, full known parent hashes for the
five replaced integration files, exact resource/build anchors, package hashes and new-file
collisions before writes. Backups precede writes; local apply failure attempts rollback.
Later build/device failure leaves source applied. Retry Validate-M3i.ps1, never Apply.
USB drop after host success: Validate-M3i.ps1 -ProjectRoot ... -BuildAndInstall -DeviceOnly
-DeviceSerial HA1B2KKW checks recorded source/receipt/APK/JUnit hashes and does NOT rebuild.
No engine/dependency/SDK/INTERNET/Git/CI changes or original source writes are included.

## Manual acceptance on a disposable text PDF
1. Confirm M3i. Select a left color, drag/adjust, tap Valider. The temporary handles disappear
   and the native highlight remains after Hand, zoom/Fit and page navigation.
2. Add another highlight using a DIFFERENT color, including on another page. Existing native
   highlight colors must not change when choosing the next tool. Check page placement.
3. Undo and Redo using the original top-row icons. Only current-session created annotations
   should change. A pending temporary selection can clear on history commands.
4. Switch documents and reopen the same unchanged source; or Back/relaunch/reopen via Recents.
   Committed highlights return. History buttons reset across a new open/restart by design.
5. Open the original PDF in another reader: it remains unchanged. External export comes in
   the next bounded stage; no new editable copy should be expected beside the original yet.
6. Recheck the accepted paragraph/list/year/diacritic examples and rotation during commit.
Return518 JVM/17-suite results and these creation/history/reopen checks. If anything fails,
keep private checkpoint directories/source and report the first error; do not Clear data.

## Verification boundary
Exact source transformations, signature/resource/literal checks, independent command/history/
identity references, synthetic PDF/annotation inspection and archive hashes are recorded in
validation.json. Java/Kotlin/Gradle/JUnit/Lint/PowerShell/Android are unavailable here. No
already-passed UI/native build is claimed. Real platform validation runs in the supplied
Windows command. Only the pinned Quad constructor and accepted native APIs are used.
