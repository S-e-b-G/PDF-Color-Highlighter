# M3m — faster edit feedback and manual source Save

Baseline: supplied current M3l export; user590 JVM/build/Lint/20 native suites/manual OK.
Expected630 JVM tests (40 new),21 native suites (one new Source Save/recovery/preview proof).
Author platform execution NOT available; runtime/manual results below remain pending.

## Faster feedback
Annotation-only edits reuse immutable same-page text snapshots when page/dimensions/copy
permission match. New source/page opens/recovery extract fresh text. Existing selector,
accent/word/handle geometry and native annotation/appearance implementations are unchanged.
Metadata and character count are not rebuilt for each annotation-only edit. Current local
byte size is updated; PDF/provider metadata remains a snapshot, refreshed by reopening.
Edit frames first use960px long edge, then existing adaptive refinement restores sharpness.
Opening/navigation retain1600px. Coordinates remain native-page based, NOT bitmap pixels.
Brief softness is a deliberate tradeoff to measure; no elapsed-time speedup promised.
Durable candidate validation/checksum/sync/publication still precedes success feedback.

Timings: PdfEditTiming queue/commit/render/cleanup; PdfEditDetail pre-count/count/raster/list;
PdfEditFeedback requestToDrawMs measures native edit request through onDraw submission. It
excludes pure selection time before the request and is NOT physical compositor presentation.
No document name/URI/quotation is logged. Toolbar acknowledgement animation stays unchanged.

## Save semantics and safety
Save confirms and replaces the CURRENT opened URI; Save As stays a separate new-file export
and does not change that source. Write grants are requested/persisted via SAF. If asked,
select exactly the same source, not a similar filename; different returned URI is refused.
Save is blocked for signed/encrypted/unverified/recovered sources according to edit policy.
Source changes are detected by full SHA256, not name/mtime. Before actual provider output,
verified/synced before+after byte backups and an immutable checksummed owner intent are
published under app noBackupFilesDir/source-save-journal-v1. Candidate is native-validated;
source is rechecked immediately before output, then output is closed and source size/SHA256
read back. No-op Save verifies identity without rewriting or adding another intent.
In-process saves serialize. External writers can still race after the last check: SAF offers
no universal atomic compare-and-swap. Avoid simultaneous editors. Immediate read-back is not
cloud upload completion; filesystem sync/rename does not guarantee hardware-power-loss safety.

A matching saved-source URI/hash intent links back to the ORIGINAL edit namespace. Thus Save,
then additional UNSAVED edits, then reopen restores the latest local head without duplication
or a false changed-source refusal. A prepared intent is a content link, not proof remote Save
completed. Do not infer saved/dirty state from local revision number; success refers to the
verified saved bytes and later local edits need another Save. Undo/Redo remains session-only.

Provider failure can leave a PARTIAL original. No blind external rollback is attempted, since
that could destroy concurrent external changes. Local checkpoints and before/after backups
are retained; Save becomes blocked until verified reopen, while Save As remains available.
If normal source opening fails, only one unique verified owned recovery namespace may open
with a conspicuous recovered label; source Save is disabled, local edits and Save As work.
A recovery button is also available after source/conflict failures. Multiple namespaces or
corrupt published heads are preserved/refused rather than guessed or rolled back to an older
head. No full multi-version chooser or backup browser is claimed in this increment.

Backups are NOT auto-deleted here. Each actual Save can retain roughly two PDF-sized backups
in addition to normal local history (existing cap256MiB per PDF). Disk usage can grow; storage
failure must occur before source output opens. Do not uninstall/clear app data during recovery.
Backup retention/settings/browser, source autosave, notes and generic imported editing remain
future work. No broad storage/INTERNET permission/dependency/SDK/engine/Git changes.

## First acceptance: DISPOSABLE local Documents copy only
1. Open a disposable TestFile-save-test.pdf; create/recolor/resize/delete. Check quicker first
   feedback, sharpness recovery, handles and no ghost/range/citation regression.
2. Save, confirm source name, authorize SAME source if asked. Inspect actual source in another
   reader for native count/color/Contents. Repeat Save without changes: no duplicates.
3. Save A; add B WITHOUT Save; close/reopen SAME source. Local view must recover A+B while
   actual source still contains A only. Save again; source should now contain A+B.
4. Undo after Save, Save, Redo, Save: verify external annotation state at each step.
5. Save As then Save: Save still targets the original displayed source, not the exported copy.
6. Cancel confirmation/authorization or choose another file: neither target must be written.
7. Externally modify disposable source, then Save: conflict refusal, retained local work,
   Save As fallback. Never automatically merge or overwrite the changed source.
8. Test unavailable/read-only provider if practical. Failed writes are NOT trusted. Local
   work stays exportable; after restart a unique verified local head can recover with source
   Save disabled. Ambiguous/corrupt recovery is refused and data preserved.
9. Retest manual Contents through resize/Undo/Redo/reopen/export, Obsidian/Docear extraction,
   uniform appearance, pinch/reverse/rotation/zoom and original toolbar. Android10 separately.
10. Collect the supplied timing tags after a few edits; compare actual first-frame/quality
    behavior, not theoretical pixel-area speedups. Report observed delay with timing lines.

## Test boundaries
40 new JVM tests cover actual file-backed source journal/backup/conflict/failure/namespace
logic and text-retention policy. Native21st suite creates its own PDF, exercises Save, owner
adoption with later unsaved highlights, Undo/Redo around Save, conflict/partial-write faults
and bounded preview rendering. It does NOT automate Android provider/picker/Compose lifecycle
UI or adversarial concurrent providers. All20 prior native suites and590 prior tests retained.
Author source/resource/lexical/package and independent reference tests are NOT Kotlin/JUnit/
Gradle/PowerShell/JNI/SAF execution. Use validator plus disposable manual acceptance before
relying on Save for valuable sources.
