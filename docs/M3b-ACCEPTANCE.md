# M3b - interactive read-only text selection preview

## Baseline and scope

User accepted M3a on Lenovo Android 11: host build/Lint/install, 161 JVM tests with no
failures/errors/skips, five instrumentation suites PASS, ordinary reader working.
User subsequently created a repository/commit; its SHA/push status is not known here.
This update never changes Git state. Exact reader baselines are the complete M2c.4
source files delivered in this conversation, which M3a did not modify. Preflight must
match these files: an intervening source edit is not silently replaced. Maintained
continuity documents are merged to preserve the user's commit/documentation work.

This increment wires the accepted M3a snapshot/selector into the current reader:
- Snapshot extraction on the existing serialized native worker after the preview frame.
- Pure selection calculations off the UI thread, one running + one replaceable pending.
- Six existing highlight-color targets become explicit READ-ONLY PREVIEW selectors.
- Actual glyph quads render as temporary Canvas overlays, not PDF annotations.
- Native-page-origin-aware mapping, stable under zoom/pan/raster refinements.
- A contextual Citation button/dialog shows the normalized quotation read-only; system
  text selection/copy is available in that dialog. No automatic clipboard write.
- Hand or tapping the active color exits preview. Other colors recolor the temporary
  overlay. Notes, Save/Save As, Undo/Redo and Settings remain unavailable.

The French status/tool accessibility labels explicitly say preview/not saved. No native
highlight/note, dirty PDF, undo entry, sidecar annotation or external save is created.
No production highlighting/history/recovery completion is claimed. This fulfills the
planned M3b read-only preview gate, not final on-release annotation creation.

## Reader behavior

After opening/changing page, the bitmap remains readable while text is extracted in
the background. The existing smoke text-count extraction still runs for the preview;
M3b adds one getBlocks snapshot extraction per accepted page, not per pan/zoom. This
known duplicate native extraction is tracked for later consolidation. It may delay a
queued native refinement/navigation, but never blocks the UI by native processing.
No glyph text, quotation, source identity or private PDF contents are logged.

Choose one of the six left colors, then drag one finger across actual text. The same
conservative M3a word/flow rules apply. A tap can preview one word. One-finger movement
in this mode selects rather than pans; Hand restores ordinary pan. Two-finger pinch
cancels the uncommitted selection and remains available. A remaining finger after
pinch cannot resume the canceled selection. ACTION_CANCEL/focus/gesture reset cannot
commit a preview. This is a temporary preview only, so even a committed gesture never
writes an annotation. Word/character adjustment handles are NOT included yet.

A final valid range shows a contextual Citation button over the top-right of the PDF
viewport. Its overlay placement and fixed single-line status avoid resizing the PDF
when results arrive. The quote dialog is read-only, not an editable Contents draft.
Native origin is subtracted explicitly; all quads share the same mapping as the page.
Same-page raster refinement preserves the preview geometry/pointer handling. Fit keeps
a completed preview mapped to the same glyphs unless a gesture reset cancels an active
drag. Pinching begins a new gesture and clears the prior preview.

Page/document changes, Back/app backgrounding and activity rotation clear the temporary
preview and return to Hand/navigation; native text DTOs can remain in the retained
ViewModel for its current page. No editing mode/selection is persisted in reading
metadata. Rotation closes the read-only quotation popup; there is no user draft to lose.
Reopening still restores the saved READING position, not a temporary selection.

## Limits and safeguards

M3a remains conservative: same native block/run only, plausible LTR line flow, large
inline gaps split, unsupported RTL/vertical/CID/GID/complex clusters refused. Some
human-valid selections across blocks/ragged paragraphs will be rejected. No OCR or
fabricated nearest-word selection. Copy-restricted, empty, complex or unavailable text
leaves the PDF readable and preview tools unavailable. Valid Unicode with a bad source
mapping cannot always be detected. No claim of universal paragraph/column inference.

M3a copied snapshot limit remains 100,000 glyphs; getBlocks/native memory is NOT bounded
by that limit. M3b accepts at most 5,000 glyphs for a visible overlay: a larger range is
refused, never silently truncated or stored. This is an engineering preview limit, not
a measured device threshold. Pure selection is linear in page/range size and coalesced
behind one worker; fast drags may show only the final result until computation catches
up. No frame-rate/latency promise. Native and pure work use separate serialized workers.
Only the current page snapshot/preview is retained; obsolete pending requests are dropped
and stale running results cannot publish after mode/page/cancel changes. Running native/
pure work is allowed to finish safely. Sustained memory/performance still needs measurement.

## Apply/build/install

Extract ZIP separately from the project. Run Apply-M3b.ps1 -ProjectRoot ... -BuildAndInstall.
Use -ValidateOnly for preflight with no edits/build. Source/payload hashes protect changes;
new-file collisions stop. Backups and created-file records are stored under .validation.
Write-phase failures attempt targeted rollback. Later build/device failure leaves source
applied: retry Validate-M3b.ps1, never Apply again. -AdbPath can specify ADB.
No wrapper/dependencies/locks/JDK/VSCode changes, PDF writes, Git operations, uninstall,
app-data clear or workspace-pruning changes. Use normal tool approvals; no admin required.
If the prior Windows test-results file lock recurs, stop other builds and retry validation
after the known daemon/sync investigation; do not change source to mask a file-lock error.

Expected JVM total: 182 = accepted 161 + 21 new mapping/queue tests.
The validator runs build/tests/Lint, installs with -r, reruns the five accepted suites,
adds PASS: selection preview, then launches the reader. Exactly one authorized tablet.

The new synthetic native-view suite tests translated native origin, drag/release mapping,
no selection-mode pan, temporary overlay pixels/clipping, ACTION_CANCEL and restored
navigation pan. It does NOT automatically test the real Compose buttons/dialog, physical
multitouch pinch, rotation/background or large real PDFs. Existing M3a proves extraction;
existing adaptive-render proof proves native raster work. Manual acceptance remains required.

## Six tablet checks (any ordinary text PDF)

1. Open a text PDF; after background text analysis, choose a left color. Its checkmark
   should appear; Hand should no longer show active navigation. Drag across a short word/
   sentence. A temporary colored selection should appear. Nothing is saved into the PDF.
2. Tap Citation, compare the exact quotation to the source, including French accents.
   Test a multiline range in one native text block and reverse drag. Different blocks may
   be refused; report representative refusal/layout, not private text. Close the dialog.
3. Choose another color: the temporary overlay recolors. Tap the same active color again
   or Hand: the preview clears and one-finger pan works. Notes/save/history stay disabled.
4. During a new selection, add a second finger and pinch. The unfinished selection must
   cancel; no overlay/annotation should be committed from the pinch. Lift either finger,
   finish the gesture and start again. Fit, zoom and clipping still work.
5. Zoom to about 2x-4x, select text after refinement, then Fit/zoom again. Overlay must
   remain aligned with text. Change page/open another PDF or rotate: preview clears,
   reading/navigation/recents/properties/page jump remain functional. No stale quotation
   from the previous page/document may appear.
6. Check one scanned/image-only page or an unavailable/restricted text layer if convenient:
   page remains readable and no fake quotation appears. No private PDF upload is needed.

Return build/JVM/six-instrumentation summary and failed checks. Keep user edits/PDFs out
of screenshots/logs; source text is deliberately not logged by this feature.

## Evidence and next

Author runtime lacks Java/Javac/Kotlin/Gradle/PowerShell/SDK/ADB; compilation, JUnit, Lint,
native/UI/device tests are NOT executed here. validation.json records only static/package
and independent Python reference checks. 182 is expected, not an observed passed result.
After acceptance, next increment can refine endpoints/interaction and introduce actual
native highlights/history only with completed-edit durability and honest save/recovery
semantics. Do not declare production annotation editing or milestone completion here.
