# M3s acceptance and installation

## Scope
Six top-right note icons are functional native PDF Text tools. Choose a color, tap
inside the page, enter text, then Valider. Annuler creates nothing. A recognized
note is selected by a tap; tap again or Modifier la note to edit. A note-color tap
recolors the selected note. Delete uses Supprimer la note and a confirmation dialog.
Use Hand to clear note/highlight selection and creation mode. Blank-page/scanned
note positioning is independent of text extraction. A long one-finger movement pans
in note mode; pinch never places a note. Native rendering, not an overlay, paints notes.

## Safety
Only app-owned recognized notes are edited. Locked, hidden, duplicate-identity,
non-palette or unrecognized imported notes are not adopted. Completed note mutations
use the same durable checkpoints and session Undo/Redo as highlights. Save/Save As,
conflicts, source backups and branch recovery use existing paths. Original untouched
until an explicit existing Save action; Save As does not rebind the document URI.
Unconfirmed text drafts survive rotation, not process termination. There is no
new autosave, settings or moving-note UI in this increment.

## Automated gate
754 JVM tests, 26 native suites, 58 workflow cases; all passing, no skipped, blocked,
known-gap, duplicate or missing workflow cases before working-app installation.
All previous 50 cases and the private full TestFile regression are retained.
New cases: notes-six-direct-colors-and-source-save; note-draft-cancel-is-noop;
note-edit-recolor-delete-undo-redo; note-save-as-preserves-source;
note-local-recovery-and-disk-first; note-draft-retained-on-rotation;
note-pinch-never-places; notes-and-highlights-share-history.
The native gesture suite additionally tests empty text, nonzero native page origin,
drag, cancellation, multi-pointer suppression and disabled input.

## How to run
Use Apply-M3s.ps1 -ProjectRoot <root> -BuildAndInstall -DeviceSerial <serial>.
After source application, retry Validate-M3s.ps1, not Apply. Add -DeviceOnly only
if the isolated host build already succeeded: source/APK/report hashes are checked.
The packaged automation subdirectory is a self-contained successor copied from the
provided current Automation-A1 export; old live kits need not be restored/modified.
PowerShell 7 is required. Scripts parse every supplied PS1 before applying source.
Backups are outside the project in <ProjectRoot>.validation/backups. No uninstall,
app data clearing, SDK/dependency/global-environment changes, Git writes or private
PDF bundling. Existing .validation/private-fixtures/m3r-TestFile.pdf must match the
accepted reference hash; optional -TestFilePath supplies the same original if missing.

## Evidence
The validator prints the isolated report directory. Send summary.json.txt and
REPORT.md.txt (or its report ZIP). A separate M3s-installation.json records production
build/install success. A failed gate keeps the installed working APK unchanged.

## Still requires independent acceptance
Real SAF/provider write-grant flows, cloud/SD failures, physical stylus/palm behavior,
subjective appearance, Teclast Android10, performance/accessibility and final Docear
and Obsidian note extraction are not proved by synthetic automation. There is no
claim of platform execution by the author: Java/Kotlin/Gradle/PowerShell/ADB absent.
