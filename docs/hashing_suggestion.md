Recommended hybrid (no-copy by default + optional copy + content hashing for deduplication)
Do mostly Option 2 (SAF-selected folders, no forced copy) but add a proper library database + content-based deduplication. This solves the exact duplicate-on-manual-open problem without doubling storage in the common case.
Here's how it would work:

Folder selection (what you already do)
User picks one or more root folders via ACTION_OPEN_DOCUMENT_TREE + takePersistableUriPermission(). Store the tree URIs (and persisted flags) in your DB. (Note: Android caps persisted grants at ~512 on Android 11+, but that's plenty for most users.)
Library building / scanning
On first import + periodic refresh (or background job):
Traverse each root with DocumentFile.fromTreeUri(...) recursively.
For every supported book file:
Parse embedded metadata + look for sibling .opf (easy with parentFile.listFiles()).
Compute a content hash (SHA-256 or even faster xxHash — libraries like net.jpountz.lz4:lz4 or just MessageDigest in chunks). This is the key to deduplication.
Store in DB: primary content:// URI, content hash, metadata, relative path (for folder view), etc.

Incremental scans are easy — check DocumentFile.lastModified() or size() to skip unchanged files.

Manual open / external VIEW intent handling (this fixes your current bug)
When the app receives a book URI (from Files app, Ghost Commander, "Open with", etc.):
Take persistable permission if it's a single document (ACTION_OPEN_DOCUMENT style).
Open the stream and compute the same content hash.
Lookup in your DB by hash:
Match found → reuse the existing library entry (update the stored URI if it differs, add reading progress, etc.). No duplicate.
No match → treat as new book (parse metadata/OPF, add to DB).
Hashing a 20 MB EPUB is < 200 ms on a modern phone; you can show a quick "Processing..." spinner the first time.


Optional "Copy to library folder" toggle (best of both worlds)
Add a setting "Copy local books to central library folder (recommended for sync & management)".
When enabled: on import or manual open, copy the file (+ OPF if present) to your existing OPDS-style folder, then use the copied URI in the DB.
This gives Calibre-like centralized control + easy folder-copy sync when the user wants it, without forcing the storage hit on everyone.

Why this is better than pure Option 1 or 2

No accidental duplicates ever (hash is the canonical identifier; different URIs for the same file are handled gracefully).
Zero extra storage unless the user explicitly enables copying.
Full metadata sorting/filtering/tags (you already parse OPF/embedded, so just cache it in the DB like any normal library app).
You can still offer a "Folders" browser view that walks the original SAF roots (users who love their filesystem keep it).
Integrates perfectly with your existing OPDS flow.
Sync across devices: either copy the central folder (if copy mode) or re-select the same folders + re-scan (if no-copy mode). Many users already use Syncthing for exactly this.

This pattern is used (in spirit) by Librera (folder scanning, no copy) and JetReader (direct folder access). The hashing step is the missing piece that prevents the exact problem you're seeing.
Implementation notes / gotchas

Hashing code is straightforward in Kotlin:Kotlinfun computeContentHash(uri: Uri): String {
    contentResolver.openInputStream(uri)?.use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    } ?: throw IOException("Cannot open file")
}Do it off the main thread + with progress if the library is huge.
Performance — For 1000+ books, initial scan is fine in background with a "Building library..." notification. Subsequent opens are instant (hash only computed for truly new files).
Calibre OPF — You're already handling this perfectly; just make sure the scanner looks for filename.opf next to filename.epub (or whatever extension) using the DocumentFile API.
Edge cases — Very large PDFs (100+ MB) → maybe hash only first 10 MB + size + lastModified as a fast pre-filter, then full hash if needed. Or give user a "Skip hash, trust filename+size" option (risky but fast).
UI — Show the user a clear "This book is already in your library" toast/snackbar when a manual open resolves to an existing hash.

This keeps Codex lightweight, respects user folder structure, eliminates the duplicate pain, and gives you the centralized option when desired. Since it's a personal/open-source project, you can even ship the no-copy + hash version first and add the copy toggle later — the DB schema change is trivial (just add the content_hash column + optional library_path).
If you want, drop a link to the specific import/DB code (or open an issue on your repo) and I can give more targeted snippets. Or if you're leaning hard toward pure copying because sync is your top priority, that's totally reasonable too — just make the copy optional and explain the trade-off in the UI.
