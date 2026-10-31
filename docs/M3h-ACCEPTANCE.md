# M3h — durable completed-PDF checkpoint foundation, not editor activation

## Accepted baseline and next step
M3g.1 is accepted by user report: build/Lint and478 JVM tests passed earlier; device-only
continuation reused that APK, verified saved results (did NOT rerun JVM), installed and
passed all15 reader suites. The real-document manual regression is reported OK.
Selection is frozen in this increment. Next product milestone is permanent native
highlights/notes with history and safe Save/SaveAs. Editing must not be activated before
completed changes can be retained without writing over the original source.

## What this increment actually delivers
A production-source DurablePdfRevisionStore for app-private non-cache checkpoint directories
and NativePdfEditTransaction that opens/disposes a separate mutable candidate,
plus JVM failure/recovery tests and a native highlight transaction proof. Reader UI, existing
source/session workspace and all selection/viewport logic remain unchanged. Save/Undo/Redo/
note controls stay unavailable; previews are still unsaved. This is an editor prerequisite,
not a disguised claim that interactive annotation editing or full recovery UI is finished.

## Store invariants
- initialize copies/validates the baseline in a FRESH owned directory. User/provider files
  are only read. Do not place this store under the read-only retention namespace; future
  production integration must use a distinct noBackupFilesDir editing namespace.
- Published PDFs are immutable. commit supplies a unique fresh staging destination; native
  writers must open/dispose a separate candidate document and never mutate the live reader
  document or a published input file. Fetch currentFile BEFORE commit; callbacks must not
  reenter store methods while its filesystem lock is held.
- Writer closes -> caller validates -> file fd.sync -> hash/size -> same-directory fresh
  rename -> synced binary head record -> delete INACTIVE slot -> fresh head rename -> verify.
- Two alternating manifest slots preserve the currently committed slot during publication.
  No platform-dependent replacement of the active slot is required. After an interrupted
  inactive-slot removal, the other slot remains recoverable. Orphan/pending files are not
  adopted just because they exist or have a high-looking sequence.
- SHA-256 footer, strict version/UUID/sequence/filename/size checks, matching base/session
  identity and full PDF rehash detect corruption. Corrupt published slot/PDF stops rather
  than silently rolling back and hiding possibly acknowledged edits. Earlier data is kept.
- Expected-head comparison rejects stale writes. A stable unique operation UUID supports
  retry of the LATEST uncertain publish without re-running its writer. The same ID must
  identify exactly the same operation, never different content. An older retry after later
  changes is stale, not silently duplicated. This is not full annotation-history replay.
- Cooperative filesystem lock serializes store operations. Native callers still must use
  the one owner worker and close candidate/validation objects before callback return.
- pruneOld is EXPLICIT after old native readers close, keeps both committed slot PDFs and
  caller pins, and preserves unknown-session/future/pending evidence. Normal maintenance
  retains two complete PDFs; interrupted/orphan/pinned material can exceed that. Nothing
  prunes automatically inside commit or touches ReadOnlyWorkspaceStore retention.
- Per-PDF limit256MiB follows the existing reader source ceiling. Complete PDF checkpoints
  involve whole-file IO; no per-gesture latency/heap promise, autosave cadence or optimized
  command journal is claimed. Future UI integration must manage checkpoint frequency and pins.
- This is same-filesystem process-interruption safety, NOT a guarantee against hardware
  power loss, storage corruption or uninstall/data-clear. Parent-directory fsync is not
  implemented, matching the existing explicit filesystem-durability boundary.

## Verification code
20 new JVM methods: independent baseline/source protection; new-instance recovery; sequence/
identity; stale writes; writer/validator/empty failures; injected interruptions after sync,
PDF publication, inactive-slot deletion and head publication; latest-operation idempotency;
older-operation rejection; manifest/PDF corruption; explicit pruning/pins/unknown material;
reinitialization/identity rejection. Existing478 stay unchanged -> expected498 methods.
New16th reader suite PASS: durable native edit creates its OWN one-page Unicode fixture,
opens a native candidate, creates a real Highlight with actual Page.search quads, saves to
fresh staging, validates/commits/reopens through a fresh store, checks Unicode Contents and
quads, simulates an interrupted later checkpoint and verifies the previous native annotation
still exists exactly once. It checks idempotent retry and unchanged original fixture bytes.
No private PDF, user annotation, automatic source URI write or original session mutation.
The accepted JNI search workaround and existing native ownership/destroy patterns remain.
The production candidate adapter rejects encrypted/signed/no-annotate/zero-page PDFs and
page-count changes; this is not a complete arbitrary-PDF feature-preservation/corruption audit.
Simulated interruption/fresh-instance tests are NOT an actual Android process-kill test.

## Install and retry
Extract separately. Apply-M3h.ps1 -ProjectRoot ... -BuildAndInstall -DeviceSerial HA1B2KKW.
Requires applied M3g.1 receipt and478 declarations; verifies all recorded source hashes,
exact anchors, new-file collisions and package integrity. Backups/typed-null writes and local
write self-test retained. This receipt additionally inventories/hashes the entire app/src
and pinned build/wrapper/lock files for stronger future source/provenance checks.
Connection is checked BEFORE starting a requested build/install run. If the tablet drops
after a successful build, do NOT rebuild: Validate-M3h.ps1 -ProjectRoot ... -BuildAndInstall
-DeviceOnly -DeviceSerial HA1B2KKW reuses the APK only when its recorded hash, source receipt,
manifest and saved test-result hashes match the successful host-validation proof. No repeated
Gradle run is necessary. Normal full retry uses Validate, never reapply after source success.
No SDK/engine/dependency/Git/CI/toolbar/selection algorithm changes are included.

## Manual acceptance
There is no new reader editing control in M3h. Confirm versionM3h and briefly recheck the
accepted reading/selection passage; the new checkpoint proof runs automatically. Return
host498/16-suite summary and any regression. After acceptance the next increment will wire
completed highlight creation into the durable editing session, followed by editable
Contents/history and staged external Save/SaveAs. Those are NOT declared complete now.

## Evidence boundary
Source checks, independent binary-codec/slot-publication references, filesystem fault cases,
native fixture parsing, XML and archive/hash checks are in validation.json. Java/Kotlin/
Gradle/JUnit/Lint/PowerShell/Android are unavailable in the generation host. Do not confuse
expected counts or Python reference results with an already executed platform build.
