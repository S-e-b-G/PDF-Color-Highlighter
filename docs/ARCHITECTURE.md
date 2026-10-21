# Current architecture - accepted M3a read-only baseline

The app remains a single Android module. Compose UI communicates with a retained
`ReaderViewModel`; the ViewModel owns the read-only native reader session across
configuration changes without retaining an Activity or View. Native document work and
disposal are serialized on the document owner worker. UI state contains copied values,
not MuPDF objects.

The reader imports an immutable baseline into app-private `noBackupFilesDir` storage.
That baseline is read-only; it is not an editable working file or a recovery journal.
Recent-document and viewport metadata are persisted separately using bounded local
metadata storage. List eviction/removal does not delete PDFs or revoke grants.

Rendering keeps a preview visible and may refine a whole-page raster after idle. Geometry
uses native logical page dimensions rather than bitmap pixels. Refinement and navigation
have separate generations; stale tickets are rejected, native operations remain
serialized, and bitmap bounds are checked before allocation. Per-raster bounds do not
constitute a hard process, heap, GPU or native-memory cap.

M3a's MuPDF structured-text adapter copies glyph code points, geometry and relevant flags
into engine-free DTOs, then destroys the native `StructuredText`. It honors copy permission
and must be called on the serialized document worker. The selection proof is not connected
to production reader touch input. It conservatively selects within supported LTR text flow
and refuses unsupported/ambiguous cases rather than inventing text.

The native annotation interoperability proof is historical synthetic-fixture validation,
not a production editor. The reader does not create or save annotations. Editing,
complete history, safe Save/Save As, autosave and mutation recovery remain future work.
