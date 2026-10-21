# M2b.1 — revised toolbars, Fit Width and viewport clipping

## Apply/build/install
Extract the ZIP. Keep one authorized tablet connected. Use the existing project path explicitly:

```powershell
$projectRoot = (Read-Host "Full path of your existing project folder").Trim().Trim('"')
$updateScript = (Read-Host "Full path of extracted Apply-M2b.1.ps1").Trim().Trim('"')
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& $updateScript -ProjectRoot $projectRoot -BuildAndInstall
```

No administrator rights are needed. Do not override organizational policy. The updater checks expected
M2b/M2a.1 markers, refuses new-file collisions and backs up affected files. Failed local writes trigger
best-effort rollback of only attempted changes. Unrelated methods, native annotation code, dependencies,
wrapper/locks, original PDFs and signing material are not changed. Do not rerun apply after success.

## Layout
Top: [6 highlight slots] [Save, Undo, Redo, Save As] [6 note slots].
Sixteen 48 dp slots total 768 dp before two equal group gaps. No target-width reduction is needed.
Bottom, tablets >=600 dp, three equal regions:
- Left: Previous, Open, Document name.
- Center: Fit Page, centered page number, Fit Width.
- Right: Hand, Settings, Next.
Equal side groups and symmetric Fit buttons keep the page number at screen center regardless of name
length. Names are ellipsized. Phones may use multiple rows. Tablet primary width <768 dp retains an
explicit layout warning, not silently compressed targets. Only the Lenovo layout is device-validated so far.

Fit Width is functional: it centers horizontally and preserves the normalized vertical reading anchor
where page clamping permits. It is an action, not a locked resize mode. After rotation, viewport state
restores as before; tap Fit Width again to refit to the new width if needed. The normal pinch ceiling
remains 8x, except where a very tall page's true width fit requires more: the ceiling then extends just
enough to permit actual width fit rather than claiming a partial fit.

## Clipping
The Compose container and AndroidView use clipToBounds. Native onDraw saves Canvas state, clips to
its width/height, draws, and restores. Bars have opaque backgrounds and higher sibling z-order.
This bounds drawing rather than merely hiding it with paint. Existing pointer handling is unchanged.

## Automated checks run on your PC/tablet
- assembleDebug, testDebugUnitTest, lintDebug; known-baseline expected JVM count: 41 (36 + 5).
- A separate debug ViewportClipInstrumentation renders a synthetic 4x bitmap inside a view on a larger
  software Canvas. All 80,000 pixels outside the view must remain untouched, and its interior must draw.
  This does not validate Compose hardware layers or replace the screenshot test.
- The script saves the clipping result, then launches the ordinary reader. It does not run private PDFs
  through a test or introduce a second PDF engine.

## Acceptance
1. Portrait/landscape: Save / Undo / Redo / Save As centered at top; Open bottom left.
2. Bottom number stays centered between Fit Page and Fit Width even with a long document name.
3. Zoom and pan to every edge: PDF must never appear in top, status or bottom bars.
4. Fit Width fills the viewport width and allows vertical pan; Fit Page restores whole-page fit.
5. Open, page navigation, pointer lifting and rotation remain functional.
Return zoomed screenshots in both orientations plus build/test/clipping output and any failed check.
If input freezes, collect scripts/Collect-ViewportLog.ps1 before restarting the app.

Save/Save As, Undo/Redo, annotation tools and Settings remain explicitly unavailable scaffolding. This
patch moves their controls, not their implementation. Hand indicates active navigation. Recents,
retained native session, durable editing persistence and sharper zoom rendering are still pending.
Author checks are listed in validation.json. No PowerShell, Kotlin/JUnit, Gradle or Android tests ran here.

API evidence:
https://developer.android.com/reference/kotlin/androidx/compose/ui/draw/clipToBounds.modifier
https://developer.android.com/reference/android/graphics/Canvas
