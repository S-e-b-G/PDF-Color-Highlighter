# M1.1 update — short Windows instructions

This is an incremental source update, not an APK. Run it against the existing working project.

## Apply, build and run
1. Extract the ZIP into a separate folder.
2. In PowerShell at your existing project root:

```powershell
$updateScript = Read-Host "Full path of the extracted Apply-M1.1.ps1"
& $updateScript -ProjectRoot (Get-Location).Path -BuildAndRun
```

If Windows blocks the inspected script, use a separate process rather than changing policy globally:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File $updateScript -ProjectRoot (Get-Location).Path -BuildAndRun
```

The script patches the existing debug proof, adds complete helper/test files, updates status/requirements,
and backs up every affected existing file under .validation/backups before writing. It validates the
expected functions/workarounds and refuses unexpected existing helper files. If local writing fails,
it attempts rollback of only this update's writes. It never changes Gradle/dependencies/locks, private
PDFs, signing material or unrelated source. The full modified proof file remains in your project.
Do not commit .validation or generated M1-results directories. Inspect the included source before running.
No Git operations, cloud calls or automatic installation of tools are performed.

With -BuildAndRun, it builds assembleDebug/testDebugUnitTest/lintDebug, selects the single authorized ADB
device, installs with -r, runs the proof once and retrieves all stage PDFs/report using binary-safe I/O.
It verifies copied PDF SHA-256 values against the report. Stop at the first error; do not change versions.
If the native proof fails, its full failure report is displayed/saved automatically. For a native process
crash, the crash buffer is saved without clearing it. Review logs for unrelated private data before sharing.
The default SDK lookup uses sdk.dir in local.properties, then LOCALAPPDATA/Android/Sdk. Optional -AdbPath
accepts an explicit already installed adb.exe. No local.properties contents are uploaded.

## Return
Send the unit-test result (known baseline expects 22), any Lint errors, and the new 00/01/03/05 PDFs plus report.
All generated results are collected in one newly created M1-results folder at the project root.

## Expected new fixtures (page 2)
- SG10 and its continuation: two visual lines, one continuous automatic Contents with one joining space.
- SG12/SG13: explicitly distinct paragraphs, Contents contains one blank line.
- At stage 3, SG10's highlight is manually changed to:
  Correction volontaire.
  Deuxième ligne conservée.
  Its geometry is expanded; this deliberate manual newline must survive stages 3/4/5.
- Stage 1/2: 9 proof highlights + 1 proof Text note; stage 3/4/5: 8 proof highlights + 1 proof Text note.
  Exclude the pre-existing fixture objects and the engine-created Popup when checking these counts.
- All automatic multi-line fixture quotations are now normalized. Standard Contents stays human-readable.
- Private quotation/sync hints are written as UTF-16BE with FE FF marker, not unmarked UTF-8.

## Important limits
Paragraph membership is explicit in this deterministic fixture. The new normalizer does not infer
paragraphs, reading order, columns, ligatures or arbitrary layouts. That remains reader/selection work.
Visible word-ending hyphens are preserved and joined without a space; only an explicit soft hyphen
between letters is removed. Manual Contents never passes through automatic normalization.
The metadata reader handles the new marked strings and the previous fixture's raw UTF-8 hints.
It is not a generic imported-PDF text decoder. Supplementary characters are unit-tested in the codec;
they are not claimed tested through PDFAnnotation Contents/JNI.

The original .debug identity, API29 minimum, palette and opacity 0.5 test value are unchanged.
Native work still uses one MuPDF engine. StructuredText.search remains avoided, and annotation.bounds
remains the snapshot accessor. No final UI, production history, autosave or recovery is claimed.

## Author validation
See validation.json. Python-level package/source/encoding checks are not Kotlin compilation, JUnit,
Android/Lint execution or downstream application testing. Those have not run in the authoring environment.
MuPDF 1.28.5 Java/JNI declarations were inspected; pdfobject.c Git blob:
1feca29f0794589b13a1734643fb6df8829967e5.
