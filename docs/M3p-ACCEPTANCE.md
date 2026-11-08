# M3p — disk-first and independent recovery versions

## Implemented source behavior (runtime acceptance pending)
- Ordinary open always copies the current provider bytes into immutable reader input; no local
  head adoption and no saved-owner alias lookup. Current disk hash is the normal Save binding.
- If different local copies exist, disk remains displayed under an explicit chooser. Continue
  with file dismisses the initial chooser. Copies locales remains available afterward; its
  Continue action reloads actual disk. Dialog dismissal leaves current displayed version.
- Selecting a local copy validates revision manifests/full file SHA and native editable PDF,
  checks exact hash again during immutable copy, then creates/initializes a FRESH durable v2 fork
  before publishing its reader frame. Its original v1/v2 namespace is never edited by selection.
- Every disk opening has a fresh v2 working namespace. Completed edits checkpoint as before.
  Choosing another version with unsaved current work uses existing Save/Continue/Cancel guard.
- Explicit recovered-copy Save retains inspected overwrite confirmation and late source-hash
  recheck/backups/readback. Normal disk editing/Save is not suspended by unrelated old copies.
- Unreadable source never silently adopts a local head. Verified-choice UI remains accessible;
  when source is unavailable even clean retained snapshots may be offered. Unknown/corrupt stores
  are preserved, not guessed or deleted. Failed choice permits trying another copy/current disk.
- source-save-journal-v1 and reader-highlight-checkpoints-v1 are preserved. New branch saves use
  source-save-journal-branches-v2 with same verified-write engine. Separate saveBranch API skips
  obsolete unique-owner alias lookup ONLY on a content-identical no-op. Source conflict,
  permission, signature/encryption/provider constraints, backups and readback remain enforced.

## Storage, performance and limitations
New heads live under reader-highlight-branches-v2/<URI SHA>/<random UUID>/<URI SHA>-<baseline SHA>.
No full URI or provider path is used as a filename. No automatic version/journal deletion added.
Storage can grow: Undo's1GiB budget is per active history, NOT a global cap on all versions or
source backups. Session Undo remains session-only; each explicitly opened copy starts new Undo.
A saved SHA marker suppresses clean heads; incomplete marker is ignored conservatively. Catalog
list deduplicates identical current contents, not physical stores. Files are not deduplicated.
Discovery checks checksummed two-slot metadata and file sizes, not every large PDF hash, avoiding
full rereads of all versions on each opening. Discovery is NOT authorization to adopt bytes;
resolve/open rehash both slots/snapshot and validates native PDF before use. Corrupt same-size
snapshots can appear in the chooser but must fail explicit opening without data loss.
Read-only reader-workspace pruning remains separate from durable recovery stores. No in-place
legacy migration or new pruning of old namespaces. Backups retained without automatic pruning.
Do not downgrade and expect an older application to understand new v2 branch workspaces.
Generic provider writes still lack universal CAS/atomicity; avoid concurrent external writers.

## Full automated gate
Expected698 JVM (21 new),22 existing native suites,47 UI/integration/probe cases.47 must PASS;
zero FAIL/BLOCKED/KNOWN_GAP before regular APK build/install. The two previous opening-policy
probes now ASSERT exact disk hash/editability. Force-stop first verifies disk then explicitly
selects old local head, followed by inspected recovered Save; no expectation of silent adoption.
Seven new workflow cases cover disk/local choice, same-baseline branch independence, editing and
saving changed disk without destroying old copy, version-switch Cancel, unreadable-source choice,
editing a recovered fork while original remains, legacy-byte preservation during fork/edit.
21 JVM tests cover branch/catalog integrity/marker state/URI isolation/stale selection and source
save conflict/no-op alias handling. All existing legacy public save call signatures retained.
The A1.3.1 bounded-window and post-rotation readiness repair remains in the driver unchanged.
Full original application source build and verified .debug package install only after full gate.
All device automation remains isolated AUTOTEST synthetic provider/files. No real SAF permission
picker/cloud/SD/external-reader/stylus/appearance coverage is implied. No source autosave added.

## Minimal real-file acceptance AFTER successful automated install
Use disposable local Documents copy. Edit without Save, reopen: actual disk first, local choice
available. Continue disk, edit/save differently, reopen old local copy: old work must still exist.
Choose disk again and Cancel unsaved switch: stay in previous version. Force-stop/reopen: disk
first, explicitly choose matching local content. Old-copy overwrite must confirm/recheck source.
Verify the actual provider/external reader and retain independent copies until satisfied.

## Execution boundary
Author checks exact collected source/hash bindings, resource/API/helper/lexical consistency,
independent storage-state references and ZIP integrity, not Kotlin/PowerShell/Android execution.
The supplied validator performs real platform acceptance; this document is not a passed run.
