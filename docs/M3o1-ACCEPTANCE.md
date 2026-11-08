# M3o.1 acceptance and boundaries

## Implemented
1 Back in reading mode: Enregistrer et quitter / Quitter sans enregistrer / Annuler. Save success
   verified before leave. Failure, cancelled authorization/replacement stay in app. Keep leaves
   source unchanged and retains completed local revisions. Existing open-document dialog intact.
2 Sharp edit refresh: copy displayed bitmap's longest edge to native edit target, minimum1600.
   Keep old bitmap until new revision raster ready. No960px first edit frame; OOM/refresh errors
   retain durable checkpoint and visible error instead of silently lowering resolution. May be slower.
3 Session Undo maximum1000, full-PDF snapshot storage budget1GiB. Latest command always retained
   even over budget. Large PDFs can hit byte limit much sooner. Not unlimited/cross-restart Undo.
4 Native new/modified app-owned highlights65%; preview166/255 with Multiply to preserve black text.
   Untouched old/third-party annotations keep their opacity. Existing already-uniform old highlight
   may require actual edit to adopt new style; no automatic rewrite when opening originals.
5 Row appearance supports consistent near-horizontal skew up to abs(dy/dx)=.03. Project glyphs,
   group existing physical bands/gaps/ellipsis logic and inverse-project. Exact stored glyph quads,
   quotation and manual Contents unchanged. Strong/mixed rotations conservatively preserve geometry.
   Large gutters and distinct rows remain split. No universal-PDF visual guarantee.
6 Hand after light pink. Highlight buttons40dp plus48dp Hand preserve former288dp highlight-group
   width; six original26dp colored circles retained. Existing footer Hand remains available.

## Explicitly pending
Disk-first opening while keeping multiple independently editable source/recovery versions needs a
separate durable namespace/index change. Current M3n opening/recovery guard behavior is RETAINED.
No local version deletion/migration is attempted in this increment. Do not infer this was fixed.

## Automated full gate
677 JVM (23 new),22 native,40 UI/integration/probe cases in isolated AUTOTEST application. Three
new branches: exit Save (verify PDF), exit keep-local/reopen (verify local head/source), failed
exit Save (verify stay/current work). Existing Back probe tests Cancel. Full mode required.
Synthetic same-UID fixture access seam remains declared, not a real SAF/grant/cloud/provider test.
Known gaps for disk-first/external-disk editability can remain. Any FAIL/BLOCKED or missing case
prevents installation of working APK by supplied validator. Source modifications retain backup.

## Minimal real-file checks after automatic install
On DISPOSABLE local Documents PDF: create/recolor/resize at current zoom; verify text remains
sharp and band gaps improve. Save and inspect in external reader. Back: Cancel, Keep/reopen,
Save/leave. Grant cancellation or protected/unwritable source must not leave after failed Save.
Confirm top Hand clears selection without zoom change. Old screenshot's exact PDF/font can still
need visual diagnosis; the synthetic word-gap heuristic is not proof all its gaps are eliminated.

## Safety / evidence
No source autosave, checkpoint batching, signature/encryption bypass or provider atomicity claim.
No SDK/engine/dependency changes. Source conflict/backups/readback and current recovery binding
retained. Backups retained without automatic pruning. Author lacks Kotlin/PowerShell/ADB runtime;
source/lexical/geometry/history/ZIP checks are not compile, JVM or tablet execution.
