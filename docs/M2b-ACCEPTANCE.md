# M2b — compact reader layout

## Apply/build/install
Extract the ZIP. Keep one authorized Lenovo connected. Use the same explicit project-root pattern:

```powershell
$projectRoot = (Read-Host "Full path of your existing project folder").Trim().Trim('"')
$updateScript = (Read-Host "Full path of extracted Apply-M2b.ps1").Trim().Trim('"')
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
& $updateScript -ProjectRoot $projectRoot -BuildAndInstall
```

The updater backs up affected files, replaces only MainActivity's Compose screen block and unused
screen imports, adds complete ReaderScreen/resources and updates status/requirements. Existing source
outside that screen block, the M2a.1 gesture implementation, native engine and M1.1 proof are preserved.
No dependency/wrapper/lock changes, PDF writes, Git operations, uninstall or data clearing are performed.
Unexpected local structures/new-file collisions stop before writing. Partial local write failures trigger
best-effort rollback of only attempted changes. Do not rerun after successful application.

## Layout
Tablet primary row, when the available width is at least 768 dp:
[6 highlight slots] [Open, Undo, Redo, Save] [6 note slots]
Every target slot remains 48 dp; leftover width forms the two group separators. No horizontal scrolling,
wrapping or shrinking of tablet primary targets is used. Notes have contrasting outlines.
Bottom row:
Previous, Document name, Page/Total, Save As, Fit, Hand, Settings, Next.

The diagnostic header is removed. The page view uses the remaining height. Pan/pinch must start inside
that viewing area; gestures beginning on a toolbar belong to the toolbar. A gesture already started in
the page may continue outside it until released, subject to normal Android/system cancellation.
A temporary read-only status line explains which controls are unavailable.

Implemented actions: Open, previous/next, Fit, existing pinch/pan/rotation restoration. Hand indicates
that navigation is the active mode; it is not a fake mode-switch button. Undo/Redo/Save, Save As,
Settings and the twelve annotation tools are deliberately disabled, with accessible descriptions and
no empty click handlers. This is tracked UI scaffolding, not completed annotation functionality.
They will be connected to real editing/save/history features in the appropriate milestones.

Phones may use multiple primary rows. A tablet narrower than 768 dp displays an explicit layout warning
and Open rather than silently hiding colors in a menu, shrinking targets or scrolling. Such a tablet's
final primary layout needs user approval. Lenovo measured widths should allow the intended one row;
actual visual verification is still required. Teclast support remains separately untested.

## Acceptance (no new setup)
1. Portrait and landscape: all twelve colors visible, primary row unwrapped, footer order correct.
2. PDF occupies substantially more space than in the diagnostic layout. No controls overlap system bars.
3. Open a document; previous/next and Fit work; long document name is ellipsized rather than displacing controls.
4. Start pan/pinch inside the page view; test both pan directions, pointer lifting and rotation again.
5. Disabled editing/settings controls do not trigger document changes or fake dialogs.
Send portrait/landscape screenshots and pass/fail. Existing test count is expected to remain 36.
The script reports the actual count from Gradle's XML reports; no JVM/Android/UI tests ran in the authoring environment.

## State and next work
M2a.1: user clarified fresh pan succeeds when started in the actual page viewport, not information header;
rotation worked in the latest test. This resolves the earlier direction complaint for that interaction
region; it does not prove the earlier isolated freeze impossible. Keep rotation in regression checks.
The supplied gesture trace shows page panning in both directions. General UI/accessibility hardening pending.

Milestone 2 is not complete: M2c adds recents (up to 50), reading-location restoration across ordinary
restarts, retained native session and working-copy persistence. High-resolution zoom rerendering/cache
also remain pending. Existing zoom still magnifies the 1600px smoke bitmap. No save/autosave/recovery,
production selection or complete history is claimed. New icons are simple project-authored vectors.
