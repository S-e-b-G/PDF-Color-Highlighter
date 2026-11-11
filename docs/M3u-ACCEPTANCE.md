# M3u — Hand after highlight deletion;80 percent highlight opacity

Requirements update approved2026-11-11: timed external autosave and configurable automatic
save-on-leaving are no longer required. The accepted durable local recovery plus explicit
exit/switch warning workflow satisfies these needs. Do not implement timers or automatic
external writes. Local recovery remains distinct from confirmed external saving; M3t's
persistent indicator and existing Save/Save As/recovery safeguards remain unchanged.

Same-color deletion: after successful durable deletion of the selected app highlight,
stopPreview enters Hand and clears the selected target/tool. No tool reset on failure.
Undo/Redo still uses the same complete PDF transactions. Note tools are not changed.

New and subsequently edited app-owned native highlights use PDF opacity0.80. Temporary
selection paint uses alpha204/255, also0.80. RGB palette and native notes are unchanged.
Existing saved annotations are not rewritten on open; Undo can restore their exact old
opacity. Recoloring/resizing/editing a managed highlight uses the current writer opacity,
as before (previously0.65, now0.80). No bulk migration or third-party opacity conversion.
Opacity is separate from RGB, not a replacement of the six palette colors.

Regression gate stays770 JVM/26 native/62 workflows. Existing delete workflow strengthened:
Hand after delete, a following drag creates no annotation, Undo/Redo exact content hashes,
and saved file contains no managed highlights after redo. Existing opacity workflow now
requires actual saved native opacity0.80 within0.005, not merely >=0.65. No skipped or
relaxed assertions. R1/R3/R4 automation fixes retained in separate verified successor.
The focused four-case gate covers deletion, opacity, rotated note draft cancellation and
save-indicator edit/Undo/Redo. Full acceptance remains mandatory before working install.

Author checks are static only; no PowerShell/Kotlin/Android execution here. Preserve
accepted770/26/62 baseline, original packages/backups and working-app data. No SDK/Git/
PDF-content export, uninstall, data-clear or dependency changes.
