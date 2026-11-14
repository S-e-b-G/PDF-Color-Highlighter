# M3x — offline Help beside Settings

Parent: accepted M3w-839a934744fd8774,798 JVM/27 native/72 workflows, Lenovo API30.
Expected successor gate:798 JVM/27 native/76 workflows. Author has not run platform tests.

Help is a question-mark icon immediately after Settings in both footer layout branches.
Existing centered navigation and48dp actions are retained. Available without an open PDF
once reader startup is ready; disabled during loading/native writes. Help is a separate
read-only dialog, not a quotation editor.18 French sections cover every public feature
family and explicitly separate implemented functions from remaining scope/limitations.
Version is shown; content is local string resources, with no network/dependency additions.
Scrollable guide, accessible section headings, persistent open state across rotation,
Return to top, Close, outside dismiss and Android Back. Closing Help never submits a quote.
No ViewModel/PDF/preferences writer callback is passed to the guide.

New4 workflows: no-PDF availability and Settings adjacency; scroll to the actual guide end,
return to top and unchanged notes/history/viewport; rotation and Back without app exit;
outside dismiss preserves selected highlight, tool and external save state. Old798/27/72
coverage and all previous R1/R3/R4/M3v/M3w fixes are retained. The original supplied TestFile
stays private in the native regression stage. No new code changes to annotation engines.
Phone/narrow footer is patched and statically audited; Lenovo portrait/landscape execution
is not a substitute for acceptance on every phone, Teclast or Android10 provider.

Help topics: opening/50 recents, reading/navigation/zoom/Hand, six highlight roles/80 percent,
selection/handles/lens/recolor/delete, quotation second tap/outside or Back/Cancel/failure,
notes, shared history, appearance repair, persisted palette, original Save, SaveAs/export,
status/asterisk, durable local recovery/exit warning, versions, properties, diagnostics,
unsupported scope/provider/external-reader limitations, and Help itself.

B/C remain satisfied by accepted durable local recovery/exit warnings; no external timer
or automatic save-on-leaving. Timestamped fallback, column continuation and compatible
imported-annotation editing are not claimed by Help or implemented in this increment.

Manual after automatic acceptance: inspect the new question-mark button beside Settings
in portrait and landscape. Open without a document, read/scroll the guide, return to top,
close using Close/Back/outside, and verify your document/tools/palette are unchanged.
Large-font/narrow-device comfort remains a manual acceptance item.
