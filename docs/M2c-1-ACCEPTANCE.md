# M2c.1 — document properties

## Apply/build/install
Extract the ZIP. Keep one authorized Lenovo connected. Use the established project folder explicitly:

```powershell
$projectRoot = (Read-Host "Full path of your existing project folder").Trim().Trim('"')
$updateScript = (Read-Host "Full path of extracted Apply-M2c.1.ps1").Trim().Trim('"')
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& $updateScript -ProjectRoot $projectRoot -BuildAndInstall
```

No administrator rights, dependencies, wrapper/lock updates, uninstall or data clear. The script validates
expected source (including an exact normalized hash of the unchanged SmokePdfSession snapshot), backs up
affected files, and attempts rollback of only its own writes on a local write failure. Do not reapply after
success. No PDF content/metadata, native annotations, geometry, gestures, fade duration or toolbar order is changed.

## Feature
Tap the filename in the lower-left region. It opens a scrollable, read-only properties dialog showing:
name, title, author, page count, size of the opened PDF byte stream, format, PDF creation/modification dates,
source-provider modification time where available, creator, producer, subject, keywords and original URI.
Values can be selected/copied. Missing values say Non renseigne (with the proper French accent in UI).

Native metadata is read once per document opening, on the same serialized engine worker as rendering,
not on the UI thread and not concurrently with disposal. The immutable metadata snapshot is passed to UI.
Opening the dialog does not run another native query or modify a document. A supported provider's source
mtime is queried separately; failures/unknown values are optional, not an opening error. The cache copy's
creation/modification times are never substituted for source dates. File creation time is not invented.

PDF date precision is preserved (year/month only when that is what was recorded). Known offsets are shown;
unknown PDF timezones are labelled explicitly. Unsupported/malformed date strings are displayed raw rather
than discarded or assigned a guessed time. Provider epoch times are shown in the device timezone with offset.
These two date types may legitimately differ. The dialog is a snapshot at open time, not live external-change
tracking or Info/XMP reconciliation. The URI is shown as URI, not converted into a fabricated filesystem path.

## Acceptance
1. Open a PDF, tap its filename, check author/page count and scroll through all fields. Fermer/Back closes it.
2. Compare one document's metadata to your desktop reader if convenient. Empty metadata must show Non renseigne.
3. Rotate while the dialog is open: it should reappear for the same document after the existing reader reload.
4. Close it and check zoom/pan, previous/next continuity, page-jump and one-second feedback still work.
5. Open a different PDF and verify that its properties replace the previous document's values.
Return build/test output and any failed check. No private PDF upload is needed; redact metadata if sharing screenshots.

The script builds/tests/Lints, installs, reruns the existing synthetic software-Canvas clipping check and
launches the reader. Expected known JVM count: 74 (64 + 10 PDF-date parser tests).
Author checks are source/package and independent parser-reference checks only, not Kotlin/Android execution.

## Remaining reader foundation
This small M2c.1 increment implements the requested properties feature. M2c.2 remains the 50-document recent
list and persistent reading-location restoration. Retained native-session lifecycle, durable editable working
copy, sharper zoom rendering and production annotation/history/save/recovery are still pending. No milestone
completion is claimed for those. M1.1 interoperability and the existing navigation/clipping fixes are retained.

## API evidence
MuPDF refs/tags/1.28.5 Document.java blob e9ebb51416b103731d0dc02673c6eabd1a5d6161:
getMetaData(String), META_FORMAT and META_INFO_* keys inspected via read-only GitHub source tool.
https://developer.android.com/reference/android/provider/DocumentsContract.Document
PDF date layout reference: https://www.verypdf.com/pdfinfoeditor/pdf-date-format.htm
All dependencies remain the existing pinned baseline; java.time APIs used here are available on minSdk 29.
