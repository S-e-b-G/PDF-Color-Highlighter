# M3r — whole-document appearance review and private real-PDF regression

## Application behavior
Uniformiser document scans all pages of CURRENT working version, not all recovery versions.
Native worker validates the local editable input, lists app-owned highlights and computes an
appearance-only plan. Review shows changed highlight count, affected pages and total scanned
pages. It does not modify PDF appearance at scan/review time. A baseline r0 checkpoint may be
initialized, with identical content; this is not an edit/Undo command. Already-correct document
shows0 changes, no confirm action. Cancel preserves current bytes/Undo/Redo/source/viewport.
Confirm binds to retained opaque ID and content SHA, validates preservation invariants, writes
one durable PDF checkpoint and one session Undo command for ALL changed pages. One Undo restores
whole pre-repair document; Redo restores repaired version. No automatic Source Save/export.
Manual Contents, automatic quotation, exact logical quads/selection endpoints, slot/id/page and
mode retained. Native replaces only app-owned highlights, not foreign entries. Existing M3q
page action retained. No stronger paint rewrite on already-uniform entries. No SDK/dependency/
source journal/recovery branch changes. All prior signing/encryption/permission/conflict guards
remain. Review cap5000 managed items; excessive reviews fail before applying changes. Large
PDF scans/validation can take time. Cancel interrupts at next check/page boundary, not in middle
of a blocking native operation. No persisted approval across process death or cross-restart Undo.

## Private full-reference test
Exact ORIGINAL supplied TestFile.pdf,470292 bytes,
SHA25647c7da7fd5125c4938ea995f72cf517ef284f2477b9bc24cc892476f5fcc3701.
Keep unrepaired original as regression; do not replace with the newly repaired exported file.
M3q numeric geometry fixture remains. M3r additionally loads the actual PDF in isolated native
suite, invokes production extraction and real text selection on heading/body/page2, checks new
appearance output, stages an extra malformed page2 owned highlight and foreign note, reviews and
repairs all pages, preserves metadata/foreign note/source hashes, renders actual fonts/pages and
samples robust internal word-gap patches, tests clean no-op, whole-document Undo/Redo.
This is NOT real SAF/cloud/SD permission/external-reader or subjective stylus testing.
Full PDF is private test data, copied from this private update archive into .validation/private-
fixtures/m3r-TestFile.pdf, then ONLY into isolated clone app/src/debug/assets/m3r-private.
Native test class is ONLY in live-kit kotlin directory and copied into the isolated clone. It is
not registered in working application's manifest. Private native test writes no PDF/text/page
images to reports, destroys its temporary files; asset remains in isolated APK/cache. Synthetic
workflow screenshot/report scope is declared separately. No private working-app PDFs are read.
Regular APK inspected for asset path and exact original PDF bytes before installation; status
records privateFixtureExcluded. The update ZIP contains the provided PDF. DO NOT COMMIT/PUBLISH
that ZIP or private fixture. Nested update .gitignore excludes private-fixture; cache has its own
.gitignore. Keep .validation/private-fixtures for future full validation, even if clearing reports.

## Gate
724 JVM (14 new planner tests);24 native (23 existing +1 private real-PDF suite);50 synthetic
UI/integration cases (2 new whole-document review Cancel/confirm/all-page Undo/Redo/Save).
All must PASS, no FAIL/BLOCKED/KNOWN_GAP before working app build/install. No old run counted as
fresh acceptance. Retain A1.3.1 readiness waits and all M3p versioned-recovery checks. Real-PDF
assertions are deliberately allowed to expose failures; do not skip/convert them to Known Gap.
Regular working APK install-r preserves data; no uninstall/pm clear. Source backup and receipt
provided. Author cannot execute Kotlin/PowerShell/Android here: static/reference/ZIP checks are
not proof of platform acceptance. Run supplied Validate-M3r, not previous versions.
