# M3f.5 — complete identifier/year headings before flow selection

## Evidence and accepted behavior
User reports AFIS/INCOSE selection now works after M3f.4. Earlier accepted accents and
zoom/rotation remain working. The supplied current M3f.4 diagnostic reports DIFFERENT_TEXT_FLOW,
not unsupported text, for the standards selection. L32/L33 share y=377.34..383.75: a 14-glyph
identifier at x=48.59..97.53 and a seven-glyph year at x=121.49..142.09. L35/L36 repeat the
split at y=395.83..402.24. The identifier and date are physically one heading but separate
native fragments. The selector omitted the first same-baseline sibling; after a body line,
the next pair could become competing next-row candidates. This is a text completeness bug.
Do not treat dropping the year as ordinary line-wrap normalization.

## Narrow change
Add InlineReferenceRows normalization AFTER the existing TextRowCoalescer and BEFORE the
copied snapshot reaches UI/selection. It joins only a numeric identifier fragment and an
isolated parenthesized four-digit year (optionally a year range and terminal punctuation)
when all of these hold:
- Ready/selectable copied rows with reliable supported glyphs and positive compatible bounds.
- Same native block/run, compatible baseline/height, a bounded inline gap (0.15..4 heights).
- A single unambiguous candidate date, plus an immediate same-block/run text continuation
  whose bounds support the combined heading within one column. Ambiguous/conflicting pairs
  and unsupported/mixed data are left unchanged.
- Only raw sourceFragments=1 rows, bounded to 2048 rows; no cascading support from merged rows.
The merge reuses actual glyph objects/quads and existing space metadata. It adds one logical
inferred word boundary if needed, NOT a made-up glyph or highlight rectangle. It retains all
glyph counts and builds one physical heading, eliminating the competing date-row branch.
No hardcoded prEN numbers, specific year or document identity is used.

The original flow resolver, column gap thresholds, native extraction ownership, accent,
synthetic-space, reviewed bullet profile, handles/retention and paint code stay unchanged.
Parenthesized years are the explicit bounded scope, not a universal table/glossary column
reader. A label with its description in a separate real table column can still be refused;
this increment does not silently broaden arbitrary cross-column reading order.
Visual wraps still join within an entry according to existing quotation policy. This is
primarily completeness/continuation repair, not a guarantee of recovering every authored
heading/paragraph newline. Manually edited Contents are untouched. No PDF mutation is added.

## Tests and expected results
24 new JVM methods cover the reported coordinates, retained year text, both entries,
reverse selection, glyph/quad/identity preservation, metadata/idempotence and conservative
rejections for different blocks/runs, gutters, unsupported text, absent/narrow/distant body,
ambiguous years and oversize snapshots. Existing 414 tests remain; expected total438.
Add PASS: inline reference rows, the thirteenth reader suite. It creates a self-authored PDF,
extracts real glyph geometry and deliberately recreates copied label/date fragments with
observed-style block/run context, then checks normalization, years, reverse selection and
unchanged input bytes. Forced copied fragmentation is explicit: this is not a claim that
MuPDF must choose exactly those groups for every font/PDF. No supplied PDF/font is bundled.

The existing debug-only INCOSE format assertion repair is preserved. If its exact old block
is still present, this installer applies the already supplied optional-spacing correction;
if its exact repaired block is present it is left untouched. No native test is disabled.

## Apply and retry
Extract separately. Apply-M3f.5.ps1 -ProjectRoot ... -BuildAndInstall.
-CheckOnly verifies without project writes. Requires the M3f.4 receipt; verifies ALL recorded
hashes, expected414 declarations, source anchors, new-file collisions and package hashes.
Backups precede writes. The proven typed-null File.Replace helper and local binary write/
replacement/rollback self-test are retained. Local apply failure attempts rollback; later
build/device failure leaves source applied. Retry Validate-M3f.5.ps1, never Apply/old repairs.
Default ADB is the supplied SGPro path. No CI/Git/dependency/SDK/wrapper/lock changes are needed.
Expected actual runtime outcome: 438 JVM tests, zero failures/errors/skips, build/Lint/install
success and thirteen reader PASS labels. These are expected, not a claimed author run.

## Tablet checks
1. Select the first reference and its description. The quotation MUST include (2013).
2. Select both prEN 9277 and prEN 9320 entries including the wrapped final line, forward and
   reverse; both years must remain. Adjust handles across the headings. Inspect actual
   highlight geometry on the detached year, not just quotation text.
3. Try starting/ending on the year fragment itself. Do not require an unrelated column.
4. Recheck AFIS/INCOSE, maîtrise/coûts/délais, ordinary selection, retained zoom/rotation,
   color continuity and column refusal. If it fails, send current diagnostic header plus
   the rows for these entries; no new source/PDF export is needed just to test.

## Verification boundary
Host exact-anchor replay, source/lexical checks, source declaration counts, supplied-layout
reference cases, synthetic PDF extraction, XML and ZIP/hash checks are recorded. Kotlin,
JUnit/Gradle/Lint, PowerShell and Android execution are unavailable here. Independent PDF/
Python reference checks are not a native/runtime pass or a zero-build-error guarantee.
