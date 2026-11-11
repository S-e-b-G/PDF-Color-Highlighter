# M3t — persistent external-save presentation

M3s accepted baseline (user reports):754 JVM /26 native /58 workflows, all PASS.
M3t adds observable content hashes, a Save-icon asterisk for content not yet externally
saved, and a persistent status label in the existing status row. It distinguishes
Original enregistré, Copie enregistrée; original inchangé, unsaved changes, recovered
local version, last failed write and an unconfirmed current hash. Labels are based on
verified content hashes, not local revision counters. Undo/Redo can return to a saved
hash. An export does not rebind the source URI or claim that the original was updated.

This is presentation only: hasUnsavedContent, Save/Save As, exit/navigation prompts,
recovery branches and source-conflict protection retain their M3s decisions. In
particular the existing external-save dirty policy still counts a verified Save As
as externally saved; the persistent label explicitly says the source was not replaced.
No autosave, settings, palette or file-format/persistence migration in this increment.
The visible label reports the most recent confirmed write in this session; it cannot
detect another application modifying an external PDF until existing save/open checks.

Gate:770 JVM (754+16),26 native (unchanged),62 workflows (58+4), all PASS. The four new
workflows check initial/edit/Undo/Redo/source Save, Save As versus original, cancel/
failed save/retry, and recovered-version/save/reopen. All old notes and private PDF
regressions remain. Native/source/hash safeguards are not loosened.

Run Apply-M3t.ps1 -ProjectRoot <root> -BuildAndInstall -DeviceSerial <serial>.
The intact earlier PdfColorHighlighter-M3s-update folder is required as the verified
parent. It is read, never changed. Its source guards validate the exact accepted base.
The new package generates a complete successor under <ProjectRoot>.validation/packages,
then delegates to the same guarded Apply/Validate scripts with updated version/test
counts and hashes. All replacement sources/automation are materialized before applying.
Retry Validate-M3t.ps1 rather than Apply; optional DeviceOnly remains hash-bound.
An applied source increment is not an installed APK: install only follows full isolated
acceptance. Parent/SDK/Git/tablet data/private PDFs are not rewritten by packaging.

Author checks: exact anchors against available original sources; generated source/XML/
resource/test inventories and archive integrity. The currently installed M3s full
snapshot is validated locally against its retained package, never reconstructed from
an older full export. No Kotlin/PowerShell/Android execution occurred here. Host parser
and actual platform acceptance execute on the user's PC/tablet. Settings/editable
palette and timed autosave remain the next implementation blocks after acceptance.
