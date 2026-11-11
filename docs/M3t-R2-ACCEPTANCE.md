# M3t-R2 — leaf-observed external-save label

R1 four-case evidence: ordinary source save, note pinch and save-indicator recovery/reopen
passed; save-indicator-original-edit-undo-redo failed. At failure the model's UNSAVED
expectation had passed, hasUnsavedContent=true, while the visible accessibility tree
still contained Original enregistré. This is a model/display discrepancy, not ellipsis
or a missing translated label. The prior pinch rejection did not recur in this run.

Correction: pass a provider from MainActivity to ReaderScreen; call it in a dedicated
ExternalSaveStatus composable that paints the label. This places observable state reads
in the leaf's composition scope instead of passing an eagerly computed enum across UI
boundaries. The provider captures the retained ViewModel, not an enum. Add a semantic
stateDescription for inspection, keeping the exact visible text assertion unchanged.
The version banner becomes M3t-R2; Gradle versionName remains M3t for gate continuity.

No change to hashes, presentation policy, save/exit/recovery behavior, native editing,
PDFs, test expectations, thresholds or dependencies. R1 clone-only hash accessor and
all62 workflows remain. The full gate is770 JVM/26 native/62 workflows, all PASS.
The repair stages a separate source/automation successor, backs up modified sources,
runs the same four focused UI cases, and only then optionally runs full acceptance
and working installation. A four-case PASS does not authorize installation by itself.

Author checks are static. No PowerShell/Kotlin/Android execution here. This is a
narrow correction for the observed stale display; platform success is not asserted
until the unchanged UI oracle and full regression suite pass on the user's tablet.
