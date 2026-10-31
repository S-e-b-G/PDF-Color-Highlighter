# M3f.3 — supplied-PDF dotless accents, synthetic gaps and reviewed legacy bullets

## Accepted baseline
User report: M3f.2 plus the diagnostic-privacy test fix built/Linted/installed successfully;
372 JVM tests, zero failures/errors/skips; eleven reader instrumentation suites PASS.
This update uses that applied receipt, not an older M3f export. No CI/Git operation is needed.

## Evidence from the two supplied pages
- The PDF's dotless U+0131 and separate U+02C6 circumflex overlap in the word maîtrise.
  M3f.2 accepted only ASCII bases, so that pair was intentionally left literal.
- In coûts the u quad extends to x=116.802 while the narrow accent ends at 115.512;
  the next t starts at 116.780. Spacing measured from the accent, rather than the base,
  can create a false word boundary. The PDF content has no actual space at that position.
- The symbol font AdvP4C4E74 maps /C15 from a custom encoding. Independent Poppler text
  extraction emits U+000F at AFIS/INCOSE, and inspection of the embedded CFF /C15 outline
  shows a filled circle. /C14 is different (hollow) and is NOT mapped by this update.
- pypdf/pdfplumber/Poppler/fontTools are host-only investigators, NOT added app engines.
  No user PDF, extracted vendor font, document metadata or identifying watermark is shipped.

## Changes
1. Allow a dotless-i base inside the EXISTING unique, overlapping spacing-accent matcher.
   Convert it to i only for composing that verified accent cluster. An unaccented/detached
   dotless ı and actual spaces remain unchanged. Geometry/ambiguity guards stay in place.
2. Preserve MuPDF's SYNTHETIC flag in copied glyph data. Suppress an ASCII space only when
   MuPDF marks it synthetic, it is adjacent to an accepted accent cluster, and the actual
   base/next letter quads touch within a tight 0.10-height tolerance. Explicit spaces,
   NBSP/narrow NBSP and real word gaps remain. Inferred metadata uses the same base-aware
   test; word snapping crosses only suppressed synthetic spaces. Raw visualText and native
   quads remain available, and synthetic glyphs are not deleted from the snapshot.
3. Use the verified 1.28.5 StructuredTextWalker callback to inspect a font name ONLY for
   U+000F. Map that scalar to BLACK CIRCLE only for the exact AdvP4C4E74 family (with an
   optional standard six-uppercase-letter subset prefix) and reliable Unicode provenance.
   Other fonts, controls, CID/GID fallbacks and hollow /C14 are untouched. This is an explicit
   reviewed legacy-font profile, not a generic bullet detector or control deletion rule.
   A font name is not cryptographic proof; broader profiles require separate evidence.
4. Keep original sourceCodePoint and synthetic provenance in copied TextGlyph data. No font
   name/native handle enters UI state or diagnostics. The callback Font wrappers are owned
   kept references in MuPDF 1.28.5 (wrap.c); release every callback Font in finally. Release
   ignored callback Images and StructuredText deterministically. Ownership remains on the
   existing serialized native worker. The glyph-count cap and copy restrictions remain.
5. Add 30 JVM tests, including the supplied word coordinates, preservation/rejection cases,
   reviewed font matching and selection behavior. Expected total 402 = 372 + 30. No existing
   test is removed, skipped or weakened. Existing privacy-test fix is preserved unchanged.
6. Add a twelfth reader instrumentation: PASS: document text repair. It builds a synthetic
   AFIS/INCOSE list using a self-authored CFF circle font, checks the native font normalization,
   cross-item quotation/newline and unchanged input bytes. It contains NO supplied PDF/font.

## Limits and safety
The exact Android result still requires device testing. Independent PDF inspection does
not substitute for JNI execution. Unknown legacy fonts remain refusals. This does not add
semantic PDF tag parsing, reading-order column hops, production annotations, editing,
history/save/autosave/recovery, toolbar changes, engine/dependency/SDK upgrades or PDF writes.
Dotless-i reconstruction is a typographic interpretation inside an accent cluster, not a
general language transliteration. Quotation interpretation never runs on manual Contents.
Other PDFs can encode nonadjacent marks, different font names or true spaces differently.

## Apply and retry
Extract the one update ZIP separately. Apply-M3f.3.ps1 -ProjectRoot ... -BuildAndInstall.
-CheckOnly checks all guards without changing the project. The script requires the corrected
M3f.2 receipt, checks all recorded raw hashes, 372 test declarations, exact replacement
anchors, new-file collisions and package hashes. Backups precede writes. A local write
failure attempts rollback. Later build/device failure leaves source applied: retry
Validate-M3f.3.ps1 -ProjectRoot ... -BuildAndInstall, not Apply/older repairs.
No original update folder is needed after this self-contained update is applied.
Default ADB uses the supplied SGPro path; -AdbPath/-DeviceSerial can override it.

## Three manual checks
1. Open supplied page 1. Select the sentence from Cela through [10]. Citation should contain
   également, dû à, démontré, maîtrise, coûts and délais with no stray internal spaces.
   Genuine word separation (including after maîtrise), line-wrap joining and paragraph
   separators must remain correct. Repeat word drag and character-handle adjustment.
2. On supplied page 2, select from AFIS through the INCOSE entry and its URL, forward and
   reverse. Both circles should be included; list items should have separate citation lines.
   Then select a third adjacent permitted item. Cross-column continuation remains refused.
3. Recheck completed-range zoom/Fit/rotation, interrupted-draft rollback, remembered color,
   ellipsis, clipping and ordinary text. If a failure remains, send the current diagnostic
   header and relevant rows. No new export/upload is needed merely to test this version.

## Verification boundary
Host source/patch checks, exact PDF glyph/encoding/outline inspection, self-authored fixture
inspection, XML/resource checks, test counts, independent quotation/geometry preservation
references and ZIP/hash checks are recorded in validation.json. Kotlin/Gradle/JUnit/Lint,
PowerShell and Android execution are unavailable here; no full-build success is claimed.
The unchanged proven build settings and small typed-null write helper are retained.
