# PDF Color Highlighter

This repository contains an Android PDF reader with an accepted read-only reading
foundation and the M3a structured-text/selection-engine proof. It is not a finished
annotation editor. The production reader does not create or save annotations.

## Current reader

- Opens local PDFs through Android's document picker and uses only read grants actually
  returned by the selected provider.
- Renders one page at a time with page navigation, fit modes, zoom/pan, and clipping that
  keeps PDF content out of the toolbars.
- Maintains recent documents and reading position; reopening is read-only and does not
  silently reopen a document at fresh application startup.
- Shows cached read-only document properties. PDF metadata dates and provider modification
  time remain distinct; unknown values are not fabricated.
- Uses one MuPDF engine, pinned at 1.28.5. Native operations and disposal are serialized.
- The application manifest declares no `INTERNET` permission. Documents are not sent to a
  service, and quotation/comment/password contents must not be logged.

The tablet toolbar layout and M2c.4 rendering behavior are part of the accepted baseline.
Annotation, Save/Save As, Undo/Redo and Settings actions remain unavailable as applicable.
This project does not implement production annotation editing, saving, autosave, mutation
history, or edit recovery.

## M3a selection proof

M3a adds copied native glyph/quad snapshots and a conservative LTR selection engine. It
supports actual-quad hit testing, forward/reverse ranges, word snapping, guarded character
selection and separate visual text versus normalized automatic quotation. The native
`StructuredText` object is destroyed before the copied DTO is returned.

The proof is exercised by JVM tests and debug instrumentation; it is not connected to
reader touch input. It refuses unsupported or ambiguous layouts rather than inventing
text. General paragraph/column reading order, RTL/vertical text, complex grapheme
segmentation, OCR and arbitrary-document robustness are not claimed. Manual highlight
Contents remain editable quotation text and are not synchronized by this proof; additional
comments belong in separate Text notes.

## Build and test on Windows

Requirements: JDK 17, Android SDK Platform 36, Build Tools 36.0.0, and the SDK path in an
untracked `local.properties` file. The Gradle 9.4.1 wrapper and its wrapper JAR are checked
in. Open the repository root in VS Code and run these commands in PowerShell:

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --stacktrace
```

The app's minimum Android version is API 29 (Android 10); compile/target SDK are 36.
Device checks for the Lenovo Tab P11 Pro (Android 11/API 30) have been reported by the
user as successful. Teclast Android 10 acceptance remains separate and is not established
by the host build.

Host validation in this workspace on 2026-10-21 completed the requested Gradle command.
Gradle marked assemble and unit-test tasks UP-TO-DATE and executed Lint. The existing
JUnit XML reports 161 tests, 0 failures, 0 errors and 0 skipped. The Lint HTML report
lists 22 warnings and 3 hints. This host run did not install or instrument a tablet;
prior device/instrumentation results are user-reported evidence.

## Safety, fixtures and licensing

`fixtures/smoke-text.pdf` is a synthetic fixture dedicated to the public domain under
CC0-1.0, as recorded in [LICENSE.md](LICENSE.md). Historical interoperability proof files
and acceptance records are retained as documentation; historical proof PDFs/results are
not part of the source baseline.

`LICENSE.md` is an internal development declaration, not a complete release notice.
The full resolved third-party license/notices and source-obligation audit is still pending;
do not treat this repository as a redistribution-ready release.
