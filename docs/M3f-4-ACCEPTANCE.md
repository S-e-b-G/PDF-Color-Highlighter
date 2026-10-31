# M3f.4 — reviewed leading native replacement-circle markers

## Observed cause
User M3f.3 report: supplied page 1 accents now OK; zoom/rotation and ordinary selection OK.
AFIS-to-INCOSE remains refused. The attached diagnostic reports READY/UNSUPPORTED_TEXT;
L5 and L7 each start at g0=U+FFFD(replacement), with zero unmapped-ID flags and zero recognized
bullets. L9/L11 and corresponding right-column item rows show the same prefix. The previous
profile handled U+000F from independent extraction, but native MuPDF exposes U+FFFD here.
This is an observed representation mismatch, not evidence for widening column/flow rules.
M3f.3's automated summary was not supplied in this turn; 402 is its source-declaration baseline,
not a newly observed passing-test claim.

## Narrow correction
- Keep the reviewed AdvP4C4E74 profile and strict optional six-uppercase-letter subset prefix.
- Preserve U+000F -> BLACK CIRCLE as before. No additional control code is accepted.
- Permit U+FFFD -> generic U+2022 BULLET ONLY when it is the first non-whitespace character
  in its native line, the font family matches exactly, and native Unicode provenance is reliable.
- The known font contains filled and hollow circular markers. When MuPDF has already replaced
  one by U+FFFD, its exact source identity is unavailable in this callback. A generic bullet is
  therefore deliberate semantic list-marker normalization, not a claimed exact glyph recovery.
  The original PDF appearance is unchanged; its existing quads and sourceCodePoint are retained.
- U+FFFD in ordinary fonts, interior text, unmatched/similar names, CID/GID fallback or absent
  prefix evidence remains refused. No unknown text is silently dropped. No global U+FFFD fix.
- Font names remain transient native-worker data and are not logged/exported/retained in DTOs.
- Accent/synthetic-space/handle/retention/geometry/column rules remain unchanged.

## Regression coverage
Twelve additional JVM methods: accepted raw/subset prefixes, unknown/interior/unreliable/
missing font rejection, unchanged controls, raw provenance and report counts, selection in
both directions using L5–L8 row heights/gaps, and refusal for an interior unknown replacement.
Expected 414 JVM tests = 402 existing + 12 new; no test deleted, skipped or ignored.
The existing twelfth native suite is strengthened: its second circle uses a separate font
resource with an explicit U+FFFD ToUnicode map. It requires actual replacement provenance,
normalized bullets, cross-item quotation and unchanged input bytes. First circle retains
legacy C15 extraction. The synthetic font remains self-authored; no user PDF/vendor font
is included. Both native representations are accepted with their documented marker semantics.

## Apply/build/retry
Extract separately. Apply-M3f.4.ps1 -ProjectRoot ... -BuildAndInstall.
-CheckOnly performs all preflight checks without modifying the project.
Requires the applied M3f.3 receipt; checks all recorded hashes, expected 402 declarations,
known previous profile and exact source anchors/new-file collisions. Backups precede writes;
local failure attempts rollback. A later build/device failure leaves source applied: retry
Validate-M3f.4.ps1, not Apply or older repairs. Typed-null File.Replace and scratch write/
replacement self-test retained. Default ADB uses the supplied SGPro path.
No SDK, dependency, engine, Git/CI or PDF mutations; app version/status become M3f.4.

## Tablet checks
1. Reopen the SAME page. Select AFIS through INCOSE and its URL, forwards and backwards.
   Citation should contain both entries, each recognized item on its own line. It may show
   standard bullet characters instead of reproducing filled/hollow source marker styling.
2. Try AFIS through a third permitted same-column item, and handle adjustment across items.
3. Check Diagnostic: the affected leading glyph should no longer be a refusal; each relevant
   item row should report a recognized bullet. Other genuine unknown symbols must not be erased.
4. Recheck the accepted maîtrise/coûts sentence, ordinary selection and zoom/rotation retention.
If it still refuses, return the current header and L5–L10 with their Unicode/exceptional lines.

## Verification boundary
The diagnostic and supplied PDF were inspected; source anchors, test counts, XML and ZIP/hash
checks plus independent mapping/geometry references are recorded in validation.json. These
are NOT Kotlin/JUnit, PowerShell or Android execution. Full native/device validation remains
part of the Windows validator. No universal missing-Unicode reconstruction claim is made.
