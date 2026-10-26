# M3f - preview continuity, tool continuity and structured citations

## Accepted baseline
User supplied complete M3e success: 298 JVM tests without failures/errors/skips, host
build/Lint/install and ten instrumentation suites PASS. Six manual handle/quotation/
pinch/rotation/responsiveness checks and ellipsis appearance passed. This is the M3f
baseline, not completion of native annotation/history/save/recovery features.

## Four requested behaviors

1. Start circle above text
Start handle anchors to the first glyph's upper-left edge, with circle/stem above.
End remains below the last glyph. dp touch radius/character-level grab offset, exact
opposite logical endpoint and clipping remain. If there is no room above in the PDF
viewport, the start circle is hidden rather than clamped onto the selected text or
drawn over toolbars. Zoom/fit with the range preserved can bring it into view.
The existing geometry tests intentionally update above/below and top-anchor expectations.

2. Preserve COMPLETED range through zoom/rotation
A per-ViewModel checkpoint stores one committed copied span bound by identity to the
CURRENT page snapshot. Valid END commits; MOVE is a draft. Pinch/cancel/layout/focus
interrupts only the draft and restores that last committed span; no PDF edit occurs.
Finger-down alone no longer replaces an existing completed range before a second
pointer can establish pinch. With no prior completed range, immediate word feedback
is preserved; normal drag word snapping and handle character adjustment stay unchanged.
A final invalid/oversized range discards the checkpoint, keeping existing final-refusal
semantics rather than resurrecting it on a later zoom. Hand/active-color exit explicitly
clears the checkpoint. A failed intermediate computation may roll back to the checkpoint.

Activity onStop cancels a draft rather than selecting Hand. Retained ViewModel survives
ordinary rotation and same-process background/foreground; the fresh View draws the same
native quads under the new viewport transform. No old View/Activity/native object is
retained. Quote/diagnostic popups still close on recreation; there is no user-edited draft.
Process death/restart, uninstall/data clear and selecting another PDF do NOT restore this
in-memory selection. Reading location remains persisted separately. It is not recovery.

3. Keep chosen tool on page navigation
Previous/Next/page jump clear the old page's range and native text snapshot, but retain
the chosen color within the same document. Reliable text analysis reactivates it without
another color tap. While loading/restricted/empty/unsupported, the color is remembered
but effective View mode is temporarily navigation, so no phantom selection is created.
Hand still explicitly exits. Opening a different document/fresh launch begins navigation.
There is no persistent per-document editing-tool preference or saved annotation state.

4. Preserve recognizable paragraph/list boundaries in automatic citations
SelectionQuotationPolicy composes automatic preview text using the existing M1.1 visual-
wrap normalizer inside each segment. Recognized list starts get ONE newline; confident
paragraph spacing gets TWO. Wrapped item/paragraph continuation lines still join with
spaces/visible-hyphen policy. Bullets, numbering, actual characters and NBSP are retained.
Manual native Contents policy is untouched; never normalize a manually corrected comment.

Recognition is conservative, not a source-plain-text or tag-tree reconstruction:
- Unicode bullets are strong list markers; dash/star/number/letter markers require nearby
  matching peers in the same column/alignment. Decimal numbers/negative values are not
  blindly treated as list markers. Prefix inspection is bounded to 64 glyphs per row.
- Native block IDs are only hints. Paragraph separators require compatible column/height,
  stronger vertical spacing and indentation/short-final-line context. Ordinary spacing
  is estimated from same-block rows or nearest compatible rows, with a minimum sample
  count. Regular double-spaced wraps should not become a paragraph per physical row.
- At most 2048 rows receive prepared layout context, weakly cached. Larger snapshots fall
  back conservatively to explicit bullet boundaries plus ordinary-wrap normalization.
- The PDF may not encode the author's semantic paragraph/list intent. The adapter still
  uses copied visual text/geometry, not semantic P/L/LI tags. Ambiguous layouts can miss
  or misclassify boundaries; verify representative citations. No universal layout claim.
- Range selection/column guards are unchanged. A gap too large for selection can still
  reject before quote formatting. Cross-column reading-order continuation remains pending.

The old measured paragraph-gap test intentionally changes its expected citation from
continuous text to a paragraph-separated string. It is a behavior requirement change,
not removal/skipping of the test. Existing normal-wrap extraction tests remain.

## Ownership/performance/privacy
Checkpoint is one current copied snapshot/span, cleared on page/document/exit/VM cleanup;
never native handles or durable PDF data. Existing one-running/one-pending pure worker and
snapshot/epoch/final-release gates remain. Native extraction/render/disposal is unchanged.
Quote preparation is bounded, weakly cached and off-UI in the existing selection worker;
first-gesture latency/heap still needs device measurement. No frame-time guarantee.
No quotations/identity/pointer endpoints are newly logged/exported or copied automatically.
No dependencies/SDK/wrapper/locks/Git/workspace/PDF writes, saved annotations/history/recovery.
APK/status version is M3f; permanent toolbar order/targets/status height are unchanged.

## Apply/build/install
Extract separately. Apply-M3f.ps1 -ProjectRoot ... -BuildAndInstall. -ValidateOnly edits
nothing. After applying once retry Validate-M3f.ps1, NOT Apply or older repair scripts.
Use the working SDK session; -AdbPath can override discovery. Installer verifies all
23 M3e receipt hashes, safety files and 298 test declarations. Exact-match source patches
preserve unrelated text/line endings; the handle helper is replaced only against its
recorded parent source. New files/collisions/drift stop. Metadata docs/requirements merge.
Backups precede writes; local write failure attempts rollback. Later build/device failure
leaves source applied with guarded .validation/M3f-applied.json receipt for retries.
No uninstall/data clear or automatic Git action. Your prior commit/source is preserved.

Expected JVM total: 328 = accepted 298 + 30 new retention/tool/quotation/placement tests.
Existing three assertions/names are intentionally updated for above-text start geometry
and preserved paragraph breaks; no test is removed/skipped. Validator builds/tests/Lints,
installs with -r, reruns ten suites and adds PASS: preview continuity.
The new synthetic suite checks first-pointer/pinch/cancel/resize retention routing and
bullet quote assembly. Real Activity rotation/background, Compose tool continuity, actual
lists/paragraphs and touch comfort remain MANUAL; no automatic pass is claimed for them.

## Six tablet checks
1. Confirm M3f. Select multiple lines: start circle above text, end below. Adjust both;
   tap-without-move unchanged. Near top edge, start may hide rather than mask selected text.
2. Complete a range, then pinch zoom in/out and Fit Page/Width. Native selection/quotation
   must remain, properly aligned. Begin a new drag, then add a finger: unfinished draft
   cancels; prior completed range restores, not a ghost new range.
3. Rotate both ways; the same completed range, quote, chosen color and reading alignment
   should survive. Home/return should preserve within the same live session. Popup can
   close; tap Citation again. Fresh restart/process death is not selection persistence.
4. With color selected, Next/Previous/page jump: range clears for new page but color stays
   selected. After text analysis, drag without retapping the tool. Image-only/restricted
   pages remain readable/temporarily navigation; another text page reactivates the color.
   Different PDF starts in navigation. Hand or active-color exit clears the range/tool.
5. Select ordinary wraps: Citation must be continuous. Select several bullet/dash/numbered
   list items: each recognized item on its own line, wrapped item body continuous. Select
   across a supported paragraph boundary: a blank separator where confidently recognized.
   Verify accents, NBSP and manual spacing/hyphen policy; report counterexamples, not full
   private PDFs. No all-format/list/tag semantic guarantee is assumed.
6. Recheck handles, responsiveness, ellipse paint, diagnostics, recents/properties/page jump,
   clipping, final rejected-range clearing and canceled-result suppression. Column reading
   continuation/native annotation creation/save/history/recovery still not implemented.

Return build/JVM/eleven-native results and failures. Do not claim full selection/editing
completion until real layouts and safe mutation persistence are addressed.

## Evidence boundary
Author lacks Java/Javac/Kotlin/Gradle/PowerShell/Android SDK/ADB. No runtime compilation,
JUnit/Lint/PowerShell/native/device pass is claimed. validation.json records static/package/
independent reference checks only. 328 is expected until user execution observes it.
Logical P/L/LI structure is distinct from visual rows:
https://pdfa.org/techniques-for-accessible-pdf-background/
https://www.section508.gov/create/pdfs/common-tags-and-usage/
