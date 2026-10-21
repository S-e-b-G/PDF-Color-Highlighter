# M2c.4 - bounded adaptive whole-page rendering

## Accepted baseline and delivery

M2c.3: user reports successful build/Lint/install, 114 JVM tests with zero failures/errors,
clipping/recent-storage/durable-workspace instrumentation PASS, and manual checks OK.
This update targets export_for_ai_2026-10-18_22-18-06-788.md. A later local source edit
will be detected by preflight; it is never silently overwritten.

One ZIP: complete changed files, Apply-M2c.4.ps1, Validate-M2c.4.ps1, manifest.json,
validation.json and this guide. No Gradle/VS Code configuration, engine, dependencies,
locks, wrapper, workspace retention, toolbar order or original PDF writes are changed.
Existing disabled editing/history/save/settings tools remain disabled.

## Windows procedure

Extract into a separate update folder, not over the app project. Use the normal
explicit project-root pattern with Apply-M2c.4.ps1 -ProjectRoot ... -BuildAndInstall.
No admin rights, upgrades, --write-locks, uninstall, app-data clear or Git actions.
-ValidateOnly runs preflight without source edits/build; -AdbPath overrides ADB discovery.
Source checks use normalized exact hashes for changed code and untouched safety files.
Maintained STATUS/DECISIONS/AGENTS/requirements documents are merged, preserving any
baseline-commit documentation updates. New-file collisions stop before writing.
Backups and a created-file list are stored under .validation/backups. Local write
failures attempt targeted rollback. A later build/device failure leaves source applied.
DO NOT reapply after source application: retry using Validate-M2c.4.ps1 instead.

Expected JVM total: 136 = accepted 114 + 22 new sizing/ticket tests. The validator runs
assembleDebug, testDebugUnitTest, lintDebug, summarizes JUnit XML, installs with -r,
runs clipping, recent storage, durable workspace and new adaptive-render instrumentation,
then launches the reader. Exactly one authorized connected tablet is required.

## Implementation and limitations

A 1600px preview still opens/navigates promptly. Physical viewport pixels, native page
aspect ratio and fit-relative zoom select long-edge tiers 1600/2048/2560/3072/3584/4094.
Refinement waits about 250ms of unchanged render demand and no active page touch.
The debounce is not a render-latency promise. Whole-page panning needs no new raster.
Zoom-out retains the existing sharper bitmap until page/document change, avoiding churn.

Viewport geometry now uses native page dimensions instead of raster dimensions.
A same-page pixel replacement calls invalidate only: no pointer reset, reconfiguration,
zoom/anchor change or reading-state write. All other geometry equations stay unchanged.
Actual visual anchors may differ fractionally from older bitmap-rounded aspect ratios.

At most one ViewModel refinement request is outstanding. Native work uses the existing
single worker and a separate refinement token: it never increments the open/navigation
generation. Main-thread content/layout/gesture tickets reject obsolete results. JNI work
already running is allowed to finish safely; it can delay navigation but cannot publish
an obsolete image. Pages and native documents are not rendered/disposed concurrently.
The current preview remains visible and interactive; refinement does not set loading.
Text extraction/metadata/workspace import are not repeated for pixel refinement.

Memory: each refined bitmap is limited to 4096 pixels per side and 8,388,608 pixels
(32 MiB ARGB_8888 payload). Actual transformed RectI bounds are checked BEFORE allocation.
Planner reserves two pixels per side for rounding. No multipage cache or retained render
history is introduced. Current+candidate can approach 64MiB of payload, BUT this is NOT
a hard process/peak heap/GPU/native bound: GC-retained old UI frames, MuPDF resources,
allocator overhead and renderer buffers add memory. Published images are never recycled
immediately while a render thread could still own them. Only unpublished stale images
are explicitly recycled. Tablet memory/performance measurement remains pending.
Refinement allocation/native failure keeps preview and shows a French warning; another
page/open can retry. No forced GC, largeHeap flag, unbounded cache or tiling is added.
Extreme zoom still has a deliberate quality cap. Scans cannot gain missing source detail.

## Automated evidence boundary

22 new JVM tests cover ratios, limits, tiers, invalid inputs and ticket invalidations.
Native instrumentation creates its own synthetic PDF, checks actual resolution growth,
allocation bounds, same-page swap without viewport callbacks/geometry changes, source
immutability and outer Canvas corners. The existing full clipping test still runs.
It does NOT establish Android hardware pinch/rotation, all callback races, sustained
heap pressure, adversarial PDFs, zero-loss recovery or production annotation behavior.
Author environment has no Java/Javac/Kotlin compiler/Gradle/PowerShell/Android SDK/ADB.
No compilation, JUnit, Lint, PowerShell, instrumentation or tablet result is claimed here.
validation.json records static/package and independent Python reference checks only.
136 is an EXPECTED source-derived count, not an observed passed test report.

## Five tablet checks: any two ordinary text PDFs A and B

1. Zoom text to roughly 2x-4x, release both fingers and allow rendering to finish.
   It should sharpen without blanking or changing reading position. No fixed completion
   time is promised; scan-only PDFs are not a good sharpness comparison.
2. Pan/zoom, Fit Page/Width and rotate repeatedly. No jump when detail arrives; unchanged
   clipping, toolbar arrangement and one-second rectangular feedback; input stays usable.
3. Start zooming, quickly change page/open B, and rotate during transitions. No old
   page/document image may replace the current page. Repeat several times.
4. Reopen A from recents: same page/full normalized reading position. Previous/Next and
   page jump still preserve zoom/proportional X and start at top. Recheck filename
   properties and the page-jump dialog draft across rotation.
5. Zoom high, zoom out, and navigate for a few minutes. Report crashes, persistent render
   warnings or frozen input. This is regression testing, not proof of a measured hard
   memory bound. If failing, report text/scanned type, page count, orientation and zoom;
   no private document upload is needed. Use the existing viewport-log collector for input.

Return build/JVM/instrumentation summary and failed checks. If successful, preserve this
increment in Git after reviewing source/test/doc changes; no automatic commit/push is run.

## Next gate

After acceptance, proceed to small production structured-text/selection increments and
native highlights/history with completed-edit persistence safeguards before enabling
mutations. Manual Save/Save As, autosave, mutation recovery, settings and wider device/
accessibility/memory acceptance remain pending. Do not declare the final editor complete.

## API evidence

Pinned MuPDF tag 1.28.5 AndroidDrawDevice.java was inspected earlier in this conversation:
blob 44d3fe03879c0c0c9a10525c479a2b06c0b6cf08, fitPage(Page,int,int), drawPage(Page,Matrix),
RectI transformed bounds and ARGB_8888 allocation. No binding inferred from Python/JS.
https://github.com/ArtifexSoftware/mupdf/blob/1.28.5/platform/java/src/com/artifex/mupdf/fitz/android/AndroidDrawDevice.java
