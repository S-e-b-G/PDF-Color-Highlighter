# M3g — flexible user-directed same-column selection

## Policy change requested by the user
A large selection is not inherently ambiguous or invalid. Earlier next-line graph guards
were too conservative for this reader: native block changes, paragraph gaps, different
font heights, indents and detached inline fragments could reject ordinary text ranges.
This increment changes that DEFAULT policy instead of adding another character exception.

## Evidence
Diag_3 reports READY and DIFFERENT_TEXT_FLOW, 46 lines/1966 glyphs. L3 is a compact bullet
fragment at x117.78..126.60, y104.66..115.84; L4 is its body at x135.78..383.84,
y104.04..116.23. The old algorithm could accept intro->L4 because its search ended before
L3's slightly lower top, but rejected intro->L5 when L3/L4 both entered its competing-next-
line set. Independent old-rule reproduction gives [intro,body] for the first-item range
and rejection for the longer range. This is not a size/character limit failure.

## New default
- Order copied lines into physical rows using overlapping vertical bands and nearby centers.
- Select the complete row interval in the column shared by the actual endpoint lines.
- Include nearby compact prefixes/indices and tightly adjacent styled fragments in left-to-
  right order. Membership uses geometry, not a particular bullet/font/native block ID.
- No maximum paragraph gap, no font-height continuity veto, and no native block/run barrier
  during vertical selection. Several fragments are no longer treated as competing choices.
- A wide header can use the explicitly chosen end column instead of rejecting the whole range.
- Explicitly disjoint columns still require a reading-order choice and are not silently joined.
  The algorithm uses endpoint overlap as its column corridor; complex tables/sidebar overlaps
  can still need inspection. This is not a universal semantic PDF reading-order engine.
- Preserve actual glyphs/quads/characters and all existing accent/synthetic-space/known-symbol
  interpretation, handle endpoints, latest-only worker and preview retention. No UI/toolbars,
  renderer, native extraction ownership, dependencies, SDK or PDF writes change.

## Remaining real limits
Start/end must resolve to actual text/near-line bands on the current page. Copy restrictions,
invalid geometry, unmapped text and unsupported complex-script/cluster guards remain. They
are not paragraph/list boundaries. Cross-page selection and auto-scroll are not added.
The resource budget increases from 5000 to 25000 preview glyphs. Larger results are refused
explicitly, never truncated. The native extraction budget remains100000 glyphs; row preparation
is capped at the same100000-line absolute ceiling, sorted once per cached snapshot. Inline
adjacency closure is linear, not repeated quadratic expansion. Device latency/heap must be
tested; no unlimited selection/performance claim. Prepared rows keep copied DTOs only.

## Tests
28 new JVM methods cover the exact split-bullet geometry, both directions, all inline markers,
paragraph gaps, font/indent/native-ID changes, other valid marker shapes, interleaved-column
exclusion, wide-header end-column choice, unsupported/copy safety, quads/identity, handles,
6000 selected glyphs,25000/25001 preview budget and2500 rows beyond the old flow limit.
Four existing tests deliberately change with the requested behavior: two old paragraph-gap
refusals now require successful complete selection; a wide-header rejection now requires
only the chosen column; the preview limit boundary becomes25000/25001. No test is deleted,
skipped or ignored. Existing438 plus28 -> expected466 JVM tests.
New fourteenth suite PASS: flexible column selection extracts a self-authored native PDF,
selects intro + three bullet items + a distant following paragraph in both directions,
checks all text/markers and unchanged source bytes. No user PDF/text/font is bundled.

## Apply/retry
Extract separately. Apply-M3g.ps1 -ProjectRoot ... -BuildAndInstall.
Requires the applied M3f.5 receipt, all recorded source hashes,438 declarations, known old
TextFlowResolver source hash, exact patch anchors, no new-file collisions, package hashes.
The typed-null atomic write routine is unchanged and scratch-tested on Windows before edits.
Backups precede writes; local failure attempts rollback. Later build/device failure leaves
source applied: retry Validate-M3g.ps1, not Apply or older repairs. -CheckOnly changes no
project files. Default ADB is the supplied SGPro path. No CI/Git approval is needed.

## Manual acceptance
1. On the newly reported page select from Le Jury through the second item, through Etudes,
   and through the following paragraph. Repeat backwards and with both handles. Intro,
   detached marker/body and complete items must remain selectable together.
2. Select a larger same-column passage across headings, list/paragraph transitions and gaps.
3. Recheck complete standards headings/years, AFIS/INCOSE, accepted accent spacing, zoom/
   rotation retention, remembered tool and clipping. Clearly separate columns stay separate.
4. Inspect Citation for complete words/items and reasonable ordering; long-range permission
   is not a claim of reconstructing all authored paragraph breaks or complex table semantics.

## Verification boundary
Old/new flow references on the supplied numeric diagnostic, conservative column cases,
patch replay, generated literal/source checks, native fixture parsing, declaration counts,
archive/hash checks are recorded in validation.json. Java/Kotlin/Gradle/JUnit/PowerShell and
Android execution are unavailable in the generation environment. These are expected runtime
counts, not an already-observed full build. The Windows command performs the real validation.
