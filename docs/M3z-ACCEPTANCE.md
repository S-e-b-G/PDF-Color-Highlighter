# M3z — zoomed page edges at reading-viewport centre

Baseline: accepted/installed M3y-R1,826 JVM/28 native/82 workflows.
Expected M3z846 JVM/29 native/86 workflows; not author-executed.

After zoom>1, Hand panning permits each page edge to reach the centre of the PDF viewing
area (excluding fixed toolbar/status/footer). Grey background becomes available beyond
all4 edges. For content at least half a viewport in size, offsets are bounded to
[viewport/2-content, viewport/2]. If an axis is shorter than half a viewport, retain its
existing whole-page-inside-screen travel; the edge can still reach centre, but the page
cannot disappear. Fit-page behavior remains unchanged and resets travel/zoom.

Small horizontal axes still start centred until deliberately dragged. Page navigation
keeps proportional horizontal travel using the new range and starts at page top as before.
Deliberate vertical movement can release a short navigated page's top pin; mostly-horizontal
jitter retains its prior top-alignment behavior. Rotation/restoration preserves normalized
reading anchors within valid bounds. Native geometry, annotation coordinates and PDF bytes
are not changed by navigation. Existing coordinate-map/gesture/selection code is retained.

Recent-document binary fields/version are unchanged. Finite vertical off-sheet anchors are
now validated like existing finite horizontal anchors; geometry clamps restoration. Old
snapshots remain readable. Nonfinite anchors still reject. No PDF/recovery migration or
external write is performed by panning. Downgrade reading of newly extended metadata is not
claimed; preserve the existing verified backup rather than manually replacing app files.

20 additional JVM cases cover4 edges, corner/bounds, fit, invalid input, focal zoom,
coordinate mapping, short pages, normalized restore/resize, navigation and recent codec.
One existing boundary assertion is intentionally updated from screen-edge clamp to
half-viewport clamp; the test is retained, not removed. Small-page/fit/navigation tests stay.
New native synthetic Canvas suite verifies4 edge states, grey/white visibility, no painting
outside viewport, same-page refinement stability and Fit reset.4 UI cases exercise actual
Hand/pinch/swipe, all edges, no PDF edit, recolor/Save stability, rotation and Next/Fit.
All inherited826/28/82, including strengthened private TestFile cross-column coverage and
save-switch fixes, remain. No SDK/library/permission, annotation edit or save policy change.

Help gains the new Hand/zoom behavior in its navigation/zoom section. This does not close
unrelated fallback/imported-annotation/provider/device/release acceptance work. B/C remain
satisfied by prior durable recovery/warning decisions; no timer is introduced.
