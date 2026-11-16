# Whole-project reconciliation —2026-11-15

Reviewed sources: PDFreader-01 through06 in chronological order; the subsequent conversation
through installed M3z; current277-file project export2026-11-15; M3z FULL/install receipt.
Assistant proposal/TODO text is not automatically a user requirement. Later explicit user
changes supersede earlier plans. Implemented source, automated acceptance and real-provider/
downstream/device acceptance are separate. See PROJECT-RECONCILIATION.csv for the full matrix.

## Conclusion

The project is not missing another large reader/editor subsystem. Two substantial functional
gaps remain: automatic authorized timestamped fallback and compatible imported annotation
editing. Source also exposed an original preservation gap: NativeHighlightEdits.replace
forced80% opacity instead of retaining an existing value. M4a implements fallback and fixes
that preservation issue together. Imported editing remains the final functional package.
No fabricated "all complete" claim and no unapproved removal of these requirements.

The original fallback contract uses LOCAL TIME and original-stem_yyyy-MM-dd_HH-mm-ss.pdf,
with a collision suffix. This package follows that contract, not the UTC+UUID draft described
in an earlier incomplete response. Permission to one PDF is not sibling creation permission.
No folder means existing durable local recovery and foreground Save As remain available.

## Corrections to earlier status

- The early native/Docear/Obsidian fixture gate DID pass (PDFreader-03). Obsidian1.12.7,
  installer1.7.7 and user-tested plugin commit693fbbc4932365b8693707d1d1f95ea1a6bb76d0 were recorded.
  Today's final production-PDF regression is still distinct; do not reopen the initial proof.
- Notes, editable palette, persistent save status, second-tap/outside quotation editing,
  Help, reference-compatible columns and extended panning are implemented/accepted through M3z.
- B/C external timer/automatic leaving-save were superseded by the user's later accepted
  durable-local/exit-warning decision; they are not unfinished implementation work.
- Discard wording must not cause new destructive cleanup: current disk/version choice can
  return to external disk bytes while retaining recoverable local work. No extra deletion UI.
- A reset-to-automatic quotation action appeared in an early assistant policy model, not as
  a later missing core feature request; manual corrections must never be silently replaced.
- Unsupported signed/encrypted/restricted files may safely refuse by the original contract.
  Do not count full unrestricted editing as a promised feature.
- OCR/freehand/stylus/palm rejection/cross-page highlights/continuous scrolling/accounts/cloud
  sync/page-content editing were explicitly excluded. No search/bookmark-management UI is
  added; preserving existing document structures remains an output-preservation requirement.

## Bounded finish plan

1. One M4a source gate with fallback/opacity/docs. No separate rebuild of accepted M3z.
2. One compatible imported Highlight/Text editing package, preserving identity, Contents,
   RGB/opacity/foreign entries and rejecting unsafe/ambiguous/locked objects.
3. One consolidated real production-PDF/provider/downstream/device/release checklist. Reuse
   existing evidence; no repeated milestone audits or unrelated modernization.

This delivery is source code, not a platform pass. Expected874 JVM/30 native/90 workflows;
only actual all-PASS isolated acceptance permits working install-r. All846/29/86 retained,
including strengthened private TestFile and rendered-frame fixture guards. Nothing in this
report waives remaining imported editing or applicable manual/release verification.
