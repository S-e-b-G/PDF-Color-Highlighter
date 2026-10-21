# M2a.1 — investigate pan direction and post-rotation input freeze

## Run
Extract the ZIP, keep the Lenovo connected, and use the same explicit project-root command pattern:

```powershell
$projectRoot = (Read-Host "Full path of your existing project folder").Trim().Trim('"')
$updateScript = (Read-Host "Full path of extracted Apply-M2a.1.ps1").Trim().Trim('"')
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& $updateScript -ProjectRoot $projectRoot -BuildAndInstall
```

This is a targeted patch, not proof that the hardware issue is resolved. It checks known M2a source
markers and backs up each affected file. It replaces only the viewport view, adds a pan tracker/tests
and log collector, and changes one MainActivity enable/disable call. It refuses unexpected new-file
collisions, updates status/requirements, and preserves geometry/math, native rendering, PDFs and all
M1.1 interoperability fixes. No dependency updates, uninstall, data clearing or tablet restart occur.
Do not rerun the apply script after it succeeds; normal Gradle/ADB commands handle further builds.

## Why
User evidence: M2a build and 29 JVM tests passed. Pinch, Fit Page and pointer-lift checks passed.
Fresh drags to reveal the top were unreliable; reversing without lifting worked around it.
Rotation lost pan/zoom once, recovered after a tablet restart. Exact cause is NOT established.
A pure geometry test suite does not validate Android touch-event delivery or lifecycle interaction.

Changes target possible input-state failures: reset gesture/detector state for each fresh DOWN,
cancel/focus/size/detach/enable transitions; separate single-pointer panning from stale scale state;
use each replacement pointer's own origin; accumulate pre-slop motion rather than discarding it.
There is still no dedicated two-finger pan. The normalized viewport transform is unchanged.
Debug-only tracing records action/pointer/viewport state under PdfViewport, never document names,
URIs, quotations, comments or PDF contents. It is temporary diagnostic scaffolding, not analytics.

## Retest
1. Zoom enough to allow vertical panning. Drag upward, lift, then drag downward to reveal the top.
   Repeat five times with separate fresh gestures, starting near the center of the PDF area.
2. Rotate portrait/landscape repeatedly (about ten changes), testing fresh pan and pinch each time.
3. Repeat Fit Page and lifting either finger during a pinch.
4. Collect the trace after this sequence even if everything works:

```powershell
& (Join-Path $projectRoot 'scripts\Collect-ViewportLog.ps1') -ProjectRoot $projectRoot
```

Return pass/fail plus the generated TXT path's file. No need to paste the entire log into chat.
If gestures freeze again, collect before restarting the app/tablet so the sequence is preserved.
If needed afterward, closing/reopening the app is a narrower recovery step than rebooting the tablet;
it is not a diagnosis or a reason to clear data. Review logs before sharing.

Known-baseline expected JVM tests: 36 (29 existing + 7 tracker tests). These do not replace device checks.
No Kotlin/JUnit/PowerShell/Android execution was possible in the authoring environment. See validation.json.
M2a is not accepted until the reported input problems are resolved or scoped with evidence. Full reader
bars, recents, retained native session, zoom-resolution rendering and production editing remain pending.

Android pointer/scale reference:
https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale
