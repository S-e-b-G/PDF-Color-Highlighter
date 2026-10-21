# M2a — first reader-foundation increment

## Run
Extract the ZIP separately. Use the explicit project path from your established Windows setup:

```powershell
$projectRoot = (Read-Host "Full path of your existing project folder").Trim().Trim('"')
$updateScript = (Read-Host "Full path of the extracted Apply-M2a.ps1").Trim().Trim('"')
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& $updateScript -ProjectRoot $projectRoot -BuildAndInstall
```

Administrator rights are unnecessary. Keep only one authorized tablet connected. If organization policy
blocks the script, do not override managed policy. Without -BuildAndInstall only the source is applied.
The script does not depend on the terminal's current directory. It backs up affected files and refuses
unexpected edits to MainActivity/strings. It does not modify dependencies, lockfiles, native proof files,
PDF originals, signing material or unrelated source. No Git/reset/uninstall/data-clear operations run.
The updater performs best-effort rollback of only its writes if local file writing fails. Do not rerun
it after a successful apply; ordinary Gradle/ADB commands are sufficient for subsequent builds.

## What changes
- Real pinch zoom (1x fit-page through 8x), focal-point preservation subject to page-edge clamping.
- One-finger navigation pan, bounded so the page cannot be lost; smaller axes remain centered.
- Fit-page button, pointer-switch/cancel handling, no dedicated two-finger panning or double-tap zoom.
- Normalized page-center/zoom restoration through activity instance state when rotating/recreating.
- Fit reset on explicit page navigation or opening a new PDF.
- Pure geometry tests for fit/clamp/focus/resize/round-trip/invalid-input handling.
- Plural character-count resource and existing toUri Lint suggestion implemented.
- Existing serialized native smoke renderer remains unchanged; no annotation edits or source writes.

## Five tablet checks
1. Open a local text PDF. Pinch in/out. Text near the pinch center should stay near it until edge clamping.
2. At >1x zoom, drag with one finger in both axes; try dragging beyond each edge. The page cannot be lost.
3. Tap Ajuster la page. The whole page should fit and be centered again.
4. Zoom/pan on page 2, rotate both ways, and wait for rendering. Page number and sensible normalized
   zoom/reading position should restore. Page edges may force a slightly different center after rotation.
5. Lift either finger during a pinch and continue panning; no jump. Previous/next should work and fit the new page.
Return pass/fail for these checks and the actual JVM count. With the known M1.1 baseline, expected tests: 29.
If a crash happens, capture its relevant logcat entry before clearing any logs. No tests are claimed executed
by the author in an Android/JDK environment; inspect validation.json for the narrower checks that ran.

## Explicit scope limits
This is M2a, not the full reader-foundation milestone. The current diagnostic UI remains temporary;
the approved permanent top/bottom bars and recents are next. There are no fake annotation controls.
Reading position across normal app restarts, a retained native ViewModel session and durable editable
working copy remain pending. Rotation currently reopens/re-renders the read-only source using the
existing SmokePdfSession. No claim of process-death edit recovery, autosave or undo/redo is made.
Zoom currently magnifies the existing max-1600px fit bitmap. This proves gestures/transforms, not final
zoom-resolution quality; high-resolution rerendering/cache strategy is a later reader increment.
The transform uses normalized rendered-page coordinates; text hit-testing/PDF-coordinate mapping and
crop/rotation selection fixtures are still selection work. Pinch/pan must not be claimed accessible to
all assistive input methods until the later accessibility checks run.

## Continuity
M1.1 remains unchanged: do not remove the StructuredText.search workaround, bounds accessor or UTF-16BE fix.
The synthetic interoperability gate passed according to the user and independent supplied-artifact checks.
Docear/Obsidian downstream tests passed by user report. The previous plugin commit-date ambiguity is not
resolved by this update; the supplied plugin artifact identifies the tested build.
Sources used for Android pointer/scale mechanics:
https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale
