# M3j — verified Save As

Baseline: attached 2026-11-01 M3i export. User accepted 518 JVM tests and 17 reader suites.
Expected: 534 JVM tests (16 new), 18 reader suites (new verified native export).
No new dependency, engine, build-stack, selection or coordinate algorithm changes.

## Scope
Save As activates the existing 48dp primary-toolbar slot. System CreateDocument chooses a
NEW PDF. Export copies the current committed immutable local checkpoint (or the baseline
if no edits), then reopens the provider URI and compares byte count and SHA-256.
The original is never opened for writing. The open reader remains bound to its original
URI/local editing session. Export is NOT a dirty-state reset and not a rebind to the copy.
Unvalidated preview selections are not exported; tap Valider first.
Save (overwrite), external autosave, persistent export tracking, conflict resolution,
recovery chooser and prior-session annotation modification are NOT implemented here.

## Safety and lifecycle
Editing, navigation and opening are blocked from picker launch through export completion.
Pending request lives in the retained ViewModel (rotation works); a result after process
death without its request is ignored. Local checkpoints remain available on same-source
reopen. Export happens on the serialized native worker; checkpoint pruning cannot race.
Copy permission and recovery state are checked. Only content URIs different from the source
and initially readable/empty are accepted. No provider file is automatically deleted.
A failed/cancelled export can leave an empty/partial destination. Failure never deletes or
invalidates local edits; retry Save As to a new destination. Read-back is immediate provider
verification, NOT transactional provider writes, cloud upload completion or power-loss proof.
Some providers may reject wt or immediate read-back; such exports are not called successful.

## Acceptance on Lenovo
1. Open TestFile.pdf. Create and Valider two highlights with different colors/pages.
2. Save As to local Documents. Expect verified-copy message; open copy in another reader.
   Confirm both native annotations/colors/quotation Contents. Original must stay unchanged.
3. Export again with the same proposed name; verify no existing file was overwritten.
4. Cancel the picker; local highlights and current Undo/Redo must still work.
5. Rotate with picker open and after export. Confirm correct document and controls recover.
6. After export, Undo a local highlight: previous exported PDF must not change. Export again
   to another file and verify the second reflects Undo. Redo must still work locally.
7. Try an unavailable/read-only provider if available. Require failure, no lost local edits,
   and a successful later retry to local Documents. Do not rely on a failed destination.
8. Open an exported copy and run Obsidian and Docear extraction as for M1.1. Actual downstream
   export interoperability is pending, not inherited automatically from the old fixture proof.
9. Briefly retest accents, AFIS/INCOSE selection, handles, zoom and rotation.
10. If process termination interrupts export, reopen source to recover local highlights and
    retry Save As. There is no promise the interrupted destination is valid.

## Author checks
Export parsed against exact current sources, all replacement anchors unique; XML/resources,
UI call sites, test declaration counts and ZIP payload/hash integrity checked here.
Independent Python byte-copy/read-back/failure reference cases are NOT Kotlin execution.
Java, Kotlin, Gradle, PowerShell and Android runtimes are absent in this environment.
The included validator performs actual compile/JUnit/Lint/install/native checks on Windows.
The new native suite uses app-private files, NOT the Android provider picker; SAF lifecycle,
provider failures and UI behavior above remain manual device checks.
