# M2c.3 - durable read-only workspace foundation

## Accepted baseline
M2c.2 is accepted: user-reported successful build/Lint/install, 94 JVM tests
with zero failures/errors, clipping and recent-storage instrumentation PASS,
and all requested manual tablet checks OK. No M2c.2 reinstallation is needed.

## Scope
This is infrastructure, not a new annotation tool or visible toolbar feature.
The engine now opens a verified, persistent app-private baseline rather than
an evictable cache copy. App noBackupFilesDir/reader-workspaces contains each
baseline.pdf and its baseline.properties identity (exact original URI, optional
name, byte count, SHA-256 and local import time). That local import time is not
a PDF/provider creation date. Existing document-properties meanings are unchanged.

The existing source limit remains 256 MiB. Input is read only. Partial input,
size-limit or disk-I/O failure does not publish an incomplete baseline. The
copy is synced, independently read back/hashed, its manifest is synced/read
back, then its directory is renamed within the same storage root. The engine
opens only that completed directory. Native loading/rendering then gates a
read-only retention marker. An invalid PDF candidate is discarded after
native cleanup; a previously accepted source is not deleted by failed import.

The fingerprint describes the bytes actually imported, not an atomic snapshot of
a provider concurrently changed by another app. That concurrent-change case is
not solved here.

No original URI is opened for output, truncated or replaced. No new engine,
permission, dependency, wrapper, lockfile, toolbar, viewport geometry, gesture,
recent-list or ViewModel changes. No private identifiers/text are logged.
The optional maintenance log says only that baseline cleanup was incomplete.

## Storage and retention
These are persistent non-cache app-private files, excluded from automatic
backup by the chosen noBackupFilesDir location. They do not survive uninstall,
Clear storage/data, or deliberate app cleanup. They are not an external backup.
File sync and checked same-filesystem rename are not a power-loss guarantee;
parent-directory fsync is not implemented here.

Normal single-reader operation retains two accepted immutable read-only
baselines: the current source plus the newest other. Maximum completed PDF
payload is normally 512 MiB; staging another allowed document can temporarily
bring it to 768 MiB, plus small metadata. Actual storage follows document size.
Disk-full errors reject the new import and preserve previous completed data.
No fabricated low-storage test result is claimed.

A process-local registry protects live copies during multiple reader-session
lifetimes. Unknown directories, unsupported/corrupt manifests, unaccepted
candidates, malformed retention markers and directories containing extra files
are not automatically pruned. Such exceptional material can exceed normal
retention bounds; it is preserved, not silently treated as expendable edits.
Only clearly marked, unpinned app-owned staging directories are cleaned on a
subsequent create. A crash before a staging marker is durable may leave evidence
for manual diagnosis. No unsaved-recovery cleanup is implemented.

A baseline is IMMUTABLE. Future editing must use a distinct working file and
remove eligibility before introducing mutations. Extra files already prevent
retention deletion. Never reuse this read-only pruning policy for dirty work.
This increment supplies a byte-based SAME/DIFFERENT/UNAVAILABLE source-comparison
helper and tests, not an active save-time conflict detector. No saving is enabled.

Ordinary startup/recent opening still rereads the selected source URI, preserving
current semantics and detecting changed bytes when a new baseline is imported.
It does not silently read an older retained copy if that source is unavailable.
There is no recovery chooser, automatic offline fallback, persistence of native
handles across process death, editable workspace or recovery of annotation edits.
These require explicit future implementation. Recents/location remain independent.

## Apply/build/install
Extract the ZIP to a separate update folder (not over the app project).
Run Apply-M2c.3.ps1 with the existing -ProjectRoot and -BuildAndInstall pattern.
Use -ValidateOnly for preflight with no changes. No admin rights or tool upgrades.
The script verifies payload hashes and the exact normalized unchanged engine
session baseline, requires M2c.2 markers and 94 existing tests, refuses new-file
collisions, backs up changed files and records newly created paths. Local
write-phase failures trigger best-effort rollback. A later build/device failure
leaves source/backups in place. Do not reapply after successful source application.
Build-only retries use Validate-M2c.3.ps1 -ProjectRoot ... -BuildAndInstall.
Neither script uninstalls, clears application data, changes Git or writes user PDFs.

Expected JVM total: 114 = accepted 94 + 20 new tests. The script builds/tests/Lints,
installs with -r, runs clipping, recent-storage and new native-workspace proof,
then launches the app. The workspace proof uses a generated synthetic one-page
PDF, checks that MuPDF can open/render its durable copy, reopens identity via a
fresh store, verifies input unchanged, tests byte comparison and retention, and
cleans only its uniquely named test directories. It does not access private PDFs.
No downstream annotation extraction rerun is required for this read-only change.

## Tablet acceptance
1. Open two arbitrary multi-page PDFs, A and B. Both should render normally.
2. In A, choose a middle page, zoom and pan; switch to B and reopen A from recents.
   Page/full reading position should restore. Check filename properties.
3. Rotate, test page jump, pinch/pan, Fit Page/Width, clipping and one-second
   feedback. Previous/Next should retain zoom/proportional X and start at top.
4. Exit with Back, relaunch, reopen a recent. Startup remains the recent list.
5. Optional: use a disposable malformed or empty .pdf. Opening must report an
   error rather than damage any original; a subsequent valid PDF must still open.
6. Optional SD-card check: after opening a disposable PDF, remove its source card.
   The current already-opened page/session should remain readable, but reopening
   from its URI can fail; no silent retained-copy fallback is promised.
Return build/test/instrumentation summaries and any failed manual check.
No private PDF or new complete project export is needed for this increment.

## Evidence and next gate
The author executed package/hash/resource checks and independent Python reference
checks only. Java/Javac/Kotlin/Gradle/PowerShell/ADB are unavailable in the author
runtime. Kotlin compilation, JUnit, Lint, native instrumentation and tablet behavior
must be established by the user's run. Expected count 114 is not a passed report.

After acceptance, the next foundation increment is bounded sharper zoom rendering;
then production text selection/annotation work can use this verified baseline.
Manual Save/Save As, coordinated editing snapshots, external conflict decisions,
autosave, mutation journaling, recovery UI and settings remain pending. M2 is not
claimed complete by this infrastructure delivery alone.

## API evidence
https://developer.android.com/training/data-storage/app-specific
https://developer.android.com/identity/data/autobackup
https://developer.android.com/reference/java/io/File
Existing MuPDF calls are preserved from the accepted fitz 1.28.5 source integration.
