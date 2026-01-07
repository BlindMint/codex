Yes — there is a better, more general fix that works for any user (not just you) and follows Android best practices. The error you're seeing is common in apps that use the Storage Access Framework (SAF) for downloads, and the correct solution is to request and persist write permission when the user picks a download folder.

The Proper, User-Friendly General Fix
When the user selects a folder for downloads (in Settings > Browse or wherever you let them choose the save location):
Use ACTION_OPEN_DOCUMENT_TREE with the correct flags to request both read and write access.
Immediately take persistable URI permissions so the app can write to that folder forever (even after app restart or device reboot).
Code you need (in your folder-picker logic):
Kotlin
// When launching the folder picker
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
    // Optional: start from a suggested location
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, someDefaultUri)
    }
    // This flag is crucial on Android 11+ to allow writing
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
}

// Start the picker
startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER)

// In onActivityResult (or the new ActivityResultLauncher way)
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICK_FOLDER) {
        val uri = data?.data ?: return
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        
        // This is the key line that makes the folder writable forever
        contentResolver.takePersistableUriPermission(uri, flags)
        
        // Save the URI (e.g., in DataStore) for later use
        saveDownloadUri(uri)
    }
}
When saving a downloaded book:
Use the persisted URI to create files:
Kotlin
val downloadUri = getSavedDownloadUri() // your persisted URI
val docFile = DocumentFile.fromTreeUri(context, downloadUri)
val newFile = docFile?.createFile("application/epub+zip", "book.epub")
newFile?.openOutputStream()?.use { output ->
    // write your downloaded bytes here
}
Why This Is the Right General Solution
Works on all Android versions (including Android 15+).
No need for dangerous MANAGE_EXTERNAL_STORAGE permission (which Google rejects for most apps).
The folder remains writable even after the app is killed or the device restarts.
Users see the normal system folder picker and grant permission exactly once.
Matches how Google Play Books, Moon+ Reader, Librera, and most other e-readers handle downloads.
Should You Add a Prompt?
Yes — add a helpful message before or after the user picks the folder:

Before picking:
"Codex needs write access to save downloaded books. Choose a folder where you want books to be stored."
If the folder is read-only (rare, but possible):
Show a dialog:
"The selected folder does not allow writing. Please choose another folder (e.g., Documents or Downloads)."
You can detect read-only by trying to create a temporary file during the picker confirmation and showing the dialog if it fails.

Summary: What You Should Do (for a Proper Release)
Update your folder-picker code to request write permission and persist it (the two lines above).
Add a short explanatory message in the UI.
Test with a few different folders (internal storage, SD card, cloud-synced folders) to confirm writes work.
Optionally, add a fallback: if the user picks a read-only folder, show the dialog and prompt to pick again.
This is the standard, Google-recommended way and will prevent the "read-only collection" error for all users — not just you.

Let me know if you want the exact Kotlin snippet tailored to your existing picker code!
