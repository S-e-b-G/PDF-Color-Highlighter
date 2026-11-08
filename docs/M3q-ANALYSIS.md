# M3q analysis of supplied TestFile.pdf

M3p report accepted:698 JVM zero failures/errors/skips;22 native suites;47/47 workflow cases;
working application INSTALLED. This does not prove universal real-PDF appearance.

The uploaded PDF contains two app-owned Highlight annotations on page1, both opacity0.65.
Saved logical and appearance arrays are identical:38 title glyph quads and645 paragraph glyph
quads. Their appearance streams paint individual glyph shapes, matching the fragmented screenshot.
Current whole-span orientation fallback can be triggered by an isolated composite accent or
narrow glyph; full dominant-plane reference finds1 isolated anomaly in the title and3 in the
paragraph under the existing strict angular consistency tolerance. The paragraph also includes
an inline fragment of roughly0.76pt height within a7.32pt body row, splitting the old builder.

New Python/float32 geometry reference on exact supplied quads yields1 title band and10 paragraph
bands. All31 sampled paragraph internal-gap midpoints fall in those reference bands. These are
geometry-reference checks, NOT native raster/device proof. New native suite supplies actual
save/render/pixel + Undo/Redo + metadata/foreign-annotation/source-preservation checks on synthetic
blank PDF using the exact numeric geometry. The real PDF's fonts/selection extraction are not
re-executed here. Manual real-file verification remains required after successful automated install.

No supplied PDF bytes, original text, watermark or device screenshot are bundled in the update.
Only numeric selection geometry is included as regressions. No PDFs/private app data requested.

Existing annotations are deliberately unchanged at opening. Use Uniformiser page for app-owned
highlights on current page, inspect result, Undo if needed, Save explicitly to write source.
