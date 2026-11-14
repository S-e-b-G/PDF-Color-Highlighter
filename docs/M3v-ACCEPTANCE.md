# M3v — persistent six-slot palette settings

Baseline: accepted M3u770 JVM/26 native/62 workflows. Expected gate:790 JVM/
27 native suites/68 workflows, all passing before working-app replacement.

Settings is enabled in the existing footer location, including without an open document.
Six semantic slots are shared by highlight/note tools. RRGGBB or#RRGGBB input, immediate
previews, invalid/duplicate rejection, per-slot/full resets. Drafts persist across rotation;
Cancel leaves preferences unchanged. Apply uses verified AtomicFile storage on the existing
I/O executor, not a new dependency or annotation persistence mechanism. Read/write failures
are visible; retry or cancel. Settings never dirty the PDF, enter document Undo, externally
save it or remove local recovery.

Native annotation RGB is now copied separately from slot identity. Optional SGCHSlot is
stored inside app-owned annotations, never in a sidecar. Legacy default-colored files are
still recognized. New annotations use current presets. Existing annotations keep actual RGB
through Contents edits, resize, Undo/Redo, Save/SaveAs/recovery and preference changes.
Only exact current-palette colors get a checkmark. Off-palette highlights have an explicit
Delete button; choosing a preset recolors instead of guessing deletion. Notes retain explicit
deletion and never delete on a same-color tool tap. Unrecognized/locked third-party objects
remain protected; this does not implement general imported-annotation editing.
M3u80 percent highlight policy and successful deletion -> Hand are retained.

Requirements B/C are satisfied by the accepted durable local recovery and exit/switch warning.
No timed external autosave or automatic external save-on-leaving is added. Timestamped fallback,
cross-column continuation, exact comment-editor interaction and final platform/downstream
acceptance are not silently marked complete by this settings increment.

Added20 JVM tests: HEX validation/normalization, duplicates, codec, exact matching,
canonical legacy constructors and per-annotation color independence from unrelated edits.
Added native suite: actual AtomicFile reload/corruption/failed-write safety, custom native RGB,
preferences independent of PDF hash, comment edits/recolor, exact Undo/Redo, recovery/export.
Added6 workflows: invalid/duplicate/cancel; UI Apply/new vs old highlight/note colors;
off-palette highlight resize/comment/delete/recolor; note edit/recolor/Undo/no same-color
deletion; rotation/reset draft/cancel; persisted preferences after process restart.
All62 old workflows and R1/R3/R4 repairs retained. Counts are expected, not author-executed.

Manual visual acceptance after automatic installation:
1. Open Settings, change a slot, preview it, Cancel: toolbar colors remain unchanged.
2. Apply a unique HEX color: both tool groups update. Create a highlight and a note.
3. Older annotations retain color. Select/edit them: exact checkmark or off-palette Delete.
   Recolor explicitly, Undo/Redo, Save/SaveAs and inspect the result if convenient.
4. Restart: preferences persist. Try per-slot/full reset and invalid/duplicate values.
5. Inspect portrait, keyboard, scrolling and dark/light color contrast.

No SDK/dependency changes, new toolbar row, Git operation, uninstall or app-data clearing.
Source backup and isolated acceptance are mandatory. Author checks are static/reference only.
