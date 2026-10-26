# M3f.1 — Unicode citation and bullet-line hardening

Baseline: supplied current M3f export. User reports 328 JVM tests, eleven reader
instrumentation checks and continuity manual tests passed. Citation accents and
some bullet rows remain failing cases.

Changes:
- Native CID/GID fallback provenance is retained per glyph without guessing mappings.
- Unsupported glyphs reject the final expanded selection, not unrelated row text.
- Row-level bidi/direction/invalid geometry and complex-cluster guards remain.
- Shared inferred-space policy preserves attached combining continuations.
- Only automatic quotations use NFC. Actual spaces, raw visual text, glyphs/quads
  and manual Contents are preserved.
- Mapped BLACK CIRCLE is recognized as a list marker.
- Existing diagnostic adds category counts, not text or actual code points.

Expected JVM total: 348 = existing 328 + 20 new tests.
Two existing decomposed-quotation expectations intentionally change to NFC.
Existing eleven reader instrumentation suites remain; no new native suite is added.
New fallback tests use copied synthetic DTOs, not native CID/GID fixture generation.

Limits:
The user's exact PDF issue has not been reproduced here. NFC cannot repair missing
or wrong font mappings. Included unknown bullets/interior glyphs still reject; they
are never silently omitted. Body-only selection after an unknown prefix can work.
Separately extracted symbols and unsupported layouts may still fail geometrically.

No original PDF writes, new engine/dependencies/SDK changes, toolbar/gesture/retention
changes, annotation editing, history/save/recovery or cross-column continuation.

Author verification:
Source review and exact-anchor patch construction only for this delivered installer.
Java/Kotlin/Gradle/PowerShell/Android execution was not performed for this delivery.
Do not treat expected counts or source application as passed runtime tests.

Acceptance:
1. Retest the same failing accented words in Citation after drag and handle adjustment.
2. Test mapped black-circle markers, multiple items and wrapped item bodies.
3. On a refused row, compare body-only selection with selection including its marker.
4. If failure remains, identify the matching row by coordinates and share its Unicode
   count line plus report header. L0-L10 may not include the problematic row.
5. Retest zoom/Fit/rotation retention, draft cancellation, page-tool continuity,
   handles, ellipsis, reverse selection, column refusal and toolbar clipping.
6. If accents remain wrong, provide a short non-sensitive expected-versus-copied
   citation example; no private PDF or full quotation is required.
