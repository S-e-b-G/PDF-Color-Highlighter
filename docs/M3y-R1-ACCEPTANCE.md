# M3y-R1 — real-reference column compatibility correction

M3y814/28/80 passed the original automated subset but the user demonstrated a real
TestFile column refusal. That manual failure reopens reference-file compatibility;
old synthetic acceptance must not be described as proving the missing behavior.

Fallback corrections only: same-column primary flow and character/permission guards stay.
Use median horizontal body-line height for gutter and gap scale, not a large heading
endpoint. Allow variable font size within one column. Connect components only over gaps
up to3 body heights (not6), preventing footer text from joining the body. Refuse unexplained
text only when it actually overlaps the horizontal two-column corridor and vertical band;
a rotated notice wholly outside both columns is excluded. Intruding rotated text, spanning
rows, third lanes, mixed RTL, short/unclear columns and insufficient gutters still reject.

Added12 unit regressions for numeric reference-like layout. Added2 UI workflows with a
self-authored large heading/outer margin fixture. The existing isolated private TestFile
native suite now tests actual first-page left-column suffix -> right-column prefix, reverse
selection, exact glyph order/coverage, handles, native save/reselect and exact Undo/Redo.
It uses the original private fixture checksum and exports no PDF, normal text or page image.
The existing private extraction/render/repair assertions remain unchanged and are not waived.
Expected full gate:826 JVM/28 native/82 workflows, plus all inherited R1 save-switch checks.

Audit A2 separately fixes historical SDK/namespace parsing. It does not bless release
packaging or claim the app repair is installed. Existing review/open items and B/C decisions
remain. The provided explicit-list UTF8 audit combiner avoids wildcard/self/binary inclusion.

Manual after successful isolated acceptance/install: repeat the reported TestFile first-page
cross-column selection, inspect the quotation and no-gutter coverage, reverse gesture,
handles, Save/reopen and Undo. A pass on this reference is still not arbitrary-layout proof.
