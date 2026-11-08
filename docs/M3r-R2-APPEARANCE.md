# M3r-R2 — appearance no-op precision

Real-PDF regression after R1 reached its post-render clean-reinspection assertion (line143),
not the heading assertion. Native managed state exposes actual paint quad coordinates, while
repair recomputes target paint from exact logical metadata. PDF Y conversion/serialization can
produce tiny Float discrepancies. Original supplied PDF shows up to0.000030517578125pt in a
float32 top-down/bottom-up roundtrip; exact data-class equality is not visual equivalence.

NativeAppearanceEquivalence compares ONLY appearance arrays, same count/order and all8 corner
coordinates within0.001pt. Used ONLY to decide no-op for explicit single/page/document Uniformiser.
Never apply tolerance to logical selection quads, IDs/Contents/mode, stale annotation state,
source/save/recovery SHA bindings or checkpoint integrity. Those guards remain unchanged.
Metadata/native consistency tolerance remains unchanged. Genuine fragmented count/geometry
changes remain edits. No annotation repaint or source write when already visually equivalent.

Native private test still requires zero changes on reinspection, and now also applies empty
plan to assert unchanged sequence/content hash/Undo/Redo. Renderer/title/body/reference checks
retained.10 new JVM tests =>734 total,24 native,50 workflows. All must PASS before install.
Use Validate-M3r-R2.ps1 supplied with this repair after application; original M3r validator
expects old724 test count and must not be used for R2 acceptance. Old update/repair packages
are preserved, not rewritten. Private PDF exclusion guard retained. No new fixture data bundled.
Author static/reference checks are not Kotlin/Windows/Android execution.
