# M2b.4 — one-second feedback and reading continuity between pages

## Apply/build/install
Extract the ZIP. Connect exactly one authorized tablet. Use the existing app project explicitly:

```powershell
$projectRoot = (Read-Host "Full path of your existing project folder").Trim().Trim('"')
$updateScript = (Read-Host "Full path of extracted Apply-M2b.4.ps1").Trim().Trim('"')
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& $updateScript -ProjectRoot $projectRoot -BuildAndInstall
```

No administrator rights, dependencies, wrapper/locks, reinstall of tools, uninstall or data clear.
The updater verifies the expected M2b.3/M2b.2 source, backs up affected files and makes targeted edits.
Partial local write failure triggers best-effort rollback of only its writes. Do not reapply after success.
Original PDFs, annotation code and strict Canvas/Compose clipping remain unchanged.

## Behavior
- Enabled-toolbar rectangular feedback fades over nominally 1000ms, not 2000ms. The circular ripple
  remains disabled locally; actions still execute immediately and repeated presses restart the fade.
- Previous/Next retain the current fit-relative zoom factor and proportional horizontal position, then
  place the destination page top at viewport top. This also applies to the page-jump dialog, which uses
  the same page-navigation method. Opening a different document still starts at normal fit/centering.
- Same-sized pages retain the actual horizontal offset. For different widths, position is mapped to
  the same fraction of the valid horizontal travel range (leftmost to rightmost allowed sheet placement).
  Bounds still prevent losing a page. When no horizontal travel exists, the neutral fraction is 0.5.
- Even a destination page shorter than the viewing area begins at the top. Horizontal pan does not
  cause it to jump vertically to center. Vertical dragging works normally once the page is tall enough
  to scroll, and normal pinch/focal-point clamping remains available.
- Fit Page explicitly returns to the old centered default. Fit Width preserves an active top start;
  otherwise its existing vertical-reading-anchor behavior remains. Neither action changes PDF content.
- Rotation saves/restores the extra top-start and navigation-anchor state. A navigation fraction remains
  active across transient layout changes (for example the loading indicator) until actual pan/zoom or Fit;
  this prevents the loading row from perturbing the requested horizontal proportion.
- Zooms above the normal 8x ceiling, possible with true width fit on very tall pages, are not silently
  reduced when carried to another page. The ceiling accommodates the carried factor for that view.

## Automated checks run by your script
assembleDebug/testDebugUnitTest/lintDebug, install, existing synthetic software-Canvas clipping test,
and ordinary reader launch. Expected known JVM count: 64 (54 + 10 navigation geometry tests).
The existing clipping test is not a full Compose or hardware gesture test.
No Kotlin/Gradle/JUnit/PowerShell/Android execution occurred in the authoring environment.

## Tablet acceptance
1. Zoom page 3, move it toward the right and scroll down. Next must show page 4's top, same zoom and
   horizontal proportion. Scroll down again, then Previous: page 3 must also start at its top, not bottom.
2. Repeat at left and center positions, and with narrow pages that can be shifted within the viewport.
3. If available, repeat across pages of different dimensions; preserved position is proportional, not
   a fixed pixel offset. The zoom factor uses the reader's fit-page-relative scale, as before.
4. Page jump uses the same continuity. Fit Page still recenters and fits the whole page afterward.
5. Rotate after changing page; pan/pinch remain responsive and toolbars remain free of PDF drawing.
6. Toolbar acknowledgement lasts about one second, without a circular ripple.
Return build/test result and any failed check. No source snapshot or PDF exports are needed unless a
specific problem requires them. Use scripts/Collect-ViewportLog.ps1 if touch input misbehaves.

Editing/save/history/settings, recents/restart location, retained native session and sharper zoom
rendering remain pending. This update implements only the requested navigation/feedback changes.
