# M3g.1 — keep writing direction in copied rows; exclude rotated overlays from horizontal ranges

## Observed regression and cause
User report: M3g builds/Lints, all466 JVM tests and fourteen reader suites pass. The new
jury/list passage works, but supplied TestFile page1 fails when extending into the author
reference line. Diag_4 reports UNSUPPORTED_TEXT, not a paragraph-gap/flow refusal.
Independent supplied-PDF geometry shows a157-glyph vertical publisher watermark at
x17.203..23.203/y264.421..708.661, centre y486.541. The author-line glyphs are at
y483.031..490.621, centre486.826. M3g grouped their centres into one physical row and
scaled inline proximity by the very tall watermark bounds, pulling unsupported rotated
content into the otherwise valid horizontal range. This is my row-association bug, not a
reason to narrow paragraph selection or another exception for a person's name.

## Small generic correction
- Add a default-true horizontal orientation flag to the copied PageTextLine DTO. No native
  handle, font name or document identity is stored. Existing Kotlin constructor/copy calls
  remain compatible; validated normalizers only create horizontal supported merged rows.
- Preserve geometry orientation already available in the native beginLine callback: wmode0
  and an almost-horizontal axis in either direction. Keep the existing positive-LTR/bidi/
  malformed-data reliability checks separate. RTL or unreadable HORIZONTAL body text is not
  silently discarded; it still reaches the existing unsupported-text check.
- Build ColumnFlowRows only from geometrically horizontal lines. Rotated/vertical side text
  remains in the snapshot/diagnostic but is not an incidental member of a horizontal range.
  It cannot distort that range's physical rows or inline adjacency closure.
- An explicitly touched non-horizontal endpoint remains unsupported; this does not add
  rotated-script selection. Horizontal ranges still span paragraphs/lists/fonts/gaps/native
  IDs exactly as in M3g. No line-height restriction is added between ordinary horizontal rows.
- No name/keyword/font whitelist, hardcoded page offset or threshold tuning for this document.
  No native API/ownership/copy/queue/handle/preview/cap/dependency/SDK/Git/PDF mutation changes.

## Tests
Twelve new JVM methods cover exact supplied watermark/author geometry, diagonal overlays,
forward/reverse range, retained source/quad data, explicitly rotated endpoints, unrelated
watermark content, and refusal for genuinely unreadable horizontal text/replacement chars.
A large-font horizontal heading stays selectable.466 existing +12 -> expected478 tests.
New fifteenth reader suite PASS: horizontal flow orientation generates a self-authored PDF
with horizontal body lines, a rotated sidebar and a diagonal overlay crossing the body.
It checks native orientation provenance, complete forward/reverse body selection, exclusion
of overlay text and unchanged input bytes. No user PDF, watermark text or embedded font is
bundled; only numeric source geometry informs regression tests.

## Apply/retry
Extract separately. Apply-M3g.1.ps1 -ProjectRoot ... -BuildAndInstall.
Requires the applied M3g receipt, all recorded hashes,466 declarations, exact source anchors,
new-file collision checks and package hashes. Typed-null atomic replacement plus scratch
create/replace/rollback-write self-test retained. Backups precede writes; local write failure
attempts rollback. A later build/device failure leaves source applied: retry Validate-M3g.1.ps1,
not Apply or old repairs. Default ADB/optional serial parameters match previous updates.
No source export, GitHub or CI action is needed merely to apply/test this package.

## Manual checks
1. Reopen supplied TestFile page1. Select the Ceci paragraph through the author-reference
   line and farther down that paragraph. Repeat backwards and with handles. Side watermark
   text must not appear in Citation or be pulled into the overlay.
2. Recheck the accepted jury/list passage through its following paragraph.
3. Recheck standards/year headings, AFIS/INCOSE, accents and zoom/rotation retention.
4. If refusal remains, send the new diagnostic; raw copied rotated rows intentionally still
   exist in it. Their presence alone does not mean they were included in the chosen range.

## Verification boundary
The supplied PDF and diagnostic were inspected; independent geometry references reproduce
M3g's contamination and the orientation-filtered result. Patch/source/literal/fixture/hash/
ZIP checks are recorded in validation.json. They are NOT Kotlin/JUnit/Gradle/Lint/PowerShell/
Android execution, unavailable in this generation host. Expected counts await the actual
Windows/device run. No universal layout or zero-build-error guarantee is asserted.
