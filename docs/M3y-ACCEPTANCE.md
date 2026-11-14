# M3y — conservative same-page LTR column continuation

Baseline: accepted M3x plus R1 automation,798 JVM/27 native/76 workflows.
Expected new gate:814 JVM/28 native/80 workflows; author execution is not claimed.

Existing same-column selection runs first and is unchanged. Only when it cannot connect
endpoints does a bounded fallback consider two left-to-right columns of horizontal text.
At least3 physical rows in each coherent component, a clear gutter of at least3 endpoint
line heights, substantial vertical overlap, no mixed RTL direction and no unexplained rows
inside the shared band. Components do not bridge gaps beyond6 endpoint line heights; at
most2048 snapshot lines in fallback. Sparse/ambiguous layouts, headings between endpoints,
third lanes, short columns and restrictive/unsupported text remain refusals, not guesses.
This is a defined two-column subset, not arbitrary layout or multi-page selection.

The gesture selects the suffix of the left column then the prefix of the right, in that
reading order even with a reverse drag. Exact original copied glyphs and QuadPoints are
retained; preview/native paint cannot fill the gutter. Existing word/cluster/unknown-text
and resource-budget guards remain. No text is synthesized to force a reading path.

Reopening a saved highlight uses stored glyph-quad order rather than native extraction
array order to recover endpoints. A bounded spatial coverage check also rejects partial
or disjoint restoration. Manual Contents and annotation RGB are preserved through handles.

16 JVM additions cover reading order, reverse/exact endpoints/handles, shuffled extraction,
insufficient/third/spanning columns, copy restrictions, unsupported included text, gutter,
same-column compatibility, preview bands and managed native restoration/disjoint rejection.
New native suite covers actual extraction, no gutter bridge, saved editable native geometry,
manual quotation resize, exact Undo/Redo, local recovery/export and unchanged source.
4 actual UI workflows cover forward/reverse gestures, lens/handles/manual Contents and
Save/reopen/select/delete/Undo. Old798/27/76 coverage is retained, including scoped R1
save-switch helpers and all Help/palette/comment/private TestFile regression checks.

Help now documents the supported cross-column subset rather than marking it unimplemented.
B/C remain satisfied by accepted durable recovery/exit warning; no external autosave timer.
Authorized timestamped fallback, generalized imported editing and final real-provider/
external-reader/device/packaging acceptance remain separate work.

Manual after complete automatic acceptance: on your own clear two-column PDF, select from
near the end of the left column into the start of the right. Check quotation order, exact
visible coverage, handles, Undo/Redo and a saved/reopened copy. Ambiguous layouts should be
refused. This acceptance does not authorize or claim universal page-layout inference.
