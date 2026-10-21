# M2b.2 — navigation refinements

## Apply/build/install
Extract the ZIP. Connect one authorized tablet. Run from PowerShell using your actual project folder:

```powershell
$projectRoot = (Read-Host "Full path of your existing project folder").Trim().Trim('"')
$updateScript = (Read-Host "Full path of extracted Apply-M2b.2.ps1").Trim().Trim('"')
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& $updateScript -ProjectRoot $projectRoot -BuildAndInstall
```

No administrator rights, dependency changes, uninstall or data clearing are needed. Do not override
managed execution policy. The updater validates the expected version, backs up affected files and
attempts rollback of only its writes on a local write failure. Do not reapply after success.

## Behavior
- Small pages start/zoom centered. Explicit horizontal dragging enables movement between the left and
  right viewport edges, without losing any part of a small page. Large-page edge clamping is unchanged.
  Vertical-small-page behavior is unchanged. Fit Page/Width recenter horizontally and reset this free-pan
  flag. Rotation preserves the flag/normalized anchor, even where the viewport center is outside the page.
- Top remains Save / Undo / Redo / Save As between the six-color groups, with 48dp slots.
- Bottom: Open + name / Fit Page, Previous, number/total, Next, Fit Width / Hand + Settings.
  Its 272dp center group has equal flexible sides; the number stays centered. Phones may use two rows.
- Tap page number to open numeric entry with OK/Cancel. Accept 1..pageCount, reject invalid/overflow input
  rather than clamping. Selecting the current page does not reload it. Cancel/Back/dismiss changes nothing.
  Dialog draft/open state survives ordinary activity recreation while the document reloads.
- Enabled toolbar presses show primary-color background fading linearly over nominally 2000ms. Repeated
  clicks restart it and execute immediately. Disabled controls do not flash. Open can cover it with the
  system picker. Dialog buttons use normal Material feedback and close immediately.
- Hand cancels any active page gesture and reaffirms navigation without resetting zoom/pan. Editing modes
  are not implemented. Save/SaveAs, Undo/Redo, annotation tools and Settings remain unavailable.
- Save As uses an original disk/pencil vector. The supplied image URLs could not be retrieved by authoring
  tools; no third-party image was copied or traced. No copyright/legal guarantee is inferred from this.
- Strict clipping, native rendering, engine/dependencies, original PDFs and M1.1 fixes are unchanged.

## Automated script steps
Build assembleDebug/testDebugUnitTest/lintDebug; expected known JVM total 54 (41 + 13). Install, run the
existing synthetic software-Canvas clipping instrumentation, and launch the ordinary reader.
The Canvas check does not replace Compose screenshots/hardware gesture checks.

## Manual acceptance
1. Fit a tall page narrower than the view; drag left and right. Fit should recenter it.
2. Rotate after moving it horizontally; position stays sensible and movement still works.
3. Check footer order and new Save As icon in portrait and landscape.
4. Page dialog: first/last/middle pages; reject 0, invalid and out-of-range input; Cancel changes no page.
   Rotate with a draft open and check draft preservation.
5. Fit, Previous/Next and Hand give fading acknowledgement; repeat presses do not leave a stuck overlay.
6. Recheck clipping, pointer lifting, Open and rotation. Save/Settings/annotation tools remain disabled.
Return build/test output, failed checks, and portrait/landscape screenshots. For input problems collect
scripts/Collect-ViewportLog.ps1 before restarting. No new PDF exports are needed in this patch.

Recents/restart reading location, retained native session, sharper zoom and production editing/history/
saving are still pending. Author checks are only source/package/resource and independent algorithm
reference checks: no PowerShell, Kotlin/JUnit, Gradle or Android execution. See validation.json.

API evidence:
https://developer.android.com/reference/kotlin/androidx/compose/animation/core/Animatable
https://developer.android.com/develop/ui/compose/side-effects
