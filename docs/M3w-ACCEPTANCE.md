# M3w — selected highlight quotation interaction

Baseline: accepted M3v790 JVM/27 native/68 workflows on Lenovo TB-J706F API30.
Expected successor gate:798 JVM/27 native/72 workflows. Platform execution is not claimed.

Tap a highlight once to select it; tap the selected highlight again to open its quotation
editor. The existing Citation button remains. Apply, outside tap or Android Back submits
a valid draft. The dialog explains this. Explicit Cancel discards the draft. A no-change
submission closes without native mutation or history entry. Invalid/busy submission does
not dismiss. A native edit failure retains the editor and draft and displays the existing
error, allowing retry or Cancel. Successful durable commit closes, even if frame refresh
subsequently fails; the existing refresh warning remains. This is internal durable editing,
not an external PDF Save. External Save/SaveAs/recovery behavior is unchanged.

The editor and field/Cancel/Apply are disabled during a write. Request/close serials are
handled once and rememberSaveable prevents stale requests reopening on ordinary rotation.
Drafts retain the existing rememberSaveable behavior. Process-death preservation of an
uncommitted editor draft is not promised. The color, geometry, shared Undo/Redo, palette,
80 percent highlight opacity and deletion -> Hand behavior remain unchanged.

8 new pure JVM policy tests plus4 new actual UI workflows. Existing790/27/68 coverage and
R1/R3/R4 repairs are retained. The outside tap is computed from a freshly observed own-app
dialog window and screen margins, not a fixed coordinate guess. Comment button actions use
refreshed nodes and actionable ancestors with no coordinate fallback. This does not alter
the existing note cancellation repair.

B/C remain satisfied by accepted local recovery and exit/switch warnings; no timer or
automatic external write. Timestamped fallback, cross-column continuation, compatible
imported annotation editing, real providers/downstream and broader device acceptance remain.

Manual after automatic acceptance: select an existing highlight, tap again, edit, tap
outside; inspect the text and Undo/Redo. Repeat with Cancel and unchanged Apply. Verify
palette colors and old highlights are unchanged. Outside/Back submission is explicitly
shown inside the dialog; choose Cancel to discard.
