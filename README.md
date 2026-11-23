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

## Reprendre le projet après une pause

- Baseline validée : M4b avec réparation R2, Lenovo TB-J706F (API 30), 886 tests JVM, 31 suites natives et 94 workflows réussis. Conserver les rapports FULL et M4b-installation.json. Ne pas confondre cette validation avec celle de modifications ultérieures.
- Pour un nouvel assistant : joindre un export récent des sources, ce README, AGENTS.md, docs/STATUS.md, requirements.csv, ainsi que le dernier résumé FULL et le reçu d’installation. Pour réparer l’automatisation, fournir aussi le paquet généré réellement référencé par .validation/M4b-generated.json. Exclure PDF privés, clés et données de récupération.
- APK debug de travail : `app/build/outputs/apk/debug/app-debug.apk` sous la racine du projet. Vérifier que l’APK correspond au rapport de validation souhaité : le fichier est remplacé par les builds suivants. Ne pas prendre l’APK isolé `.autotest`. Le niveau minimum déclaré est API 29 ; le Teclast M40 reste à tester. Une mise à jour d’une installation existante doit conserver la même signature ; ne pas désinstaller pour contourner un refus.
- Obsidian : extraction réelle signalée OK. Docear : non retesté avec cette sortie, à vérifier séparément si utilisé.

## M4c.2 — PDF-XChange notes

Select a compatible parent note and choose a color. The note and replies retain their rich text and links. The thread text is read-only in this app. Deleting a parent with direct replies requires confirmation. Nested, protected and cross-page reply deletion is refused. Acceptance is pending until the isolated gate passes.

Working APK for compatible devices: `app\build\outputs\apk\debug\app-debug.apk`. Do not use the isolated `.autotest` APK. A later build may overwrite the working APK.

## How to request the next release

> Start from the last accepted version and latest source export. Implement [requested changes]. Deliver one versioned ZIP and one paste-ready VSCode PowerShell block. Reuse the full isolated runner and add automated regression checks. Update AGENTS.md, README.md (with the relative working APK path) and in-app help for user-visible changes. Print the validation report location on pass and fail. Offer installation only after the full gate passes. Include a validate-only retry that does not reapply. If a required input is missing, name the exact file; do not substitute a partial patch.
