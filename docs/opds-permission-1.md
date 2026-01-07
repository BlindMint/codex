Yes, there is a better general fix that works reliably for all users (not just you) and follows Android's recommended practices. The core issue is that the current code likely opens the directory with read-only access, which prevents creating new files (downloading books).

Recommended General Solution
The proper way to handle this on modern Android is:

When asking the user to select a download/storage directory, explicitly request write permission using the correct flags.
Persist those permissions so the app can keep using the directory across app restarts.
Use the persisted URI to create files reliably.
Correct way to open the directory picker
Kotlin
private fun openDirectoryPicker() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        // Very important: request write permission
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
        
        // Optional: suggest a starting location (e.g. Documents folder)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val documentsUri = DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:Documents"
            )
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentsUri)
        }
    }
    
    // Use Activity Result API (preferred over deprecated startActivityForResult)
    directoryPickerLauncher.launch(intent)
}
Saving the permission (in your ActivityResult callback)
Kotlin
// Using Activity Result API
private val directoryPickerLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.let { treeUri ->
            // This is the most important line
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or 
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            
            // Take persistable permission (survives app restart)
            contentResolver.takePersistableUriPermission(treeUri, flags)
            
            // Save the URI for later use (DataStore, SharedPreferences, Room, etc.)
            savePersistedUri(treeUri)
            
            // Now you can use this URI to create files
        }
    }
}
Creating a file in the selected directory
Kotlin
fun createFileInDirectory(treeUri: Uri, fileName: String, mimeType: String = "application/epub+zip"): Uri? {
    return try {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        
        DocumentsContract.createDocument(
            contentResolver,
            documentUri,
            mimeType,
            fileName
        )
    } catch (e: Exception) {
        Log.e("Storage", "Failed to create document", e)
        null
    }
}
Why this is better than just telling the user to pick a different folder
Works in almost all situations (internal storage, SD card, USB drives, some cloud providers)
Survives app restarts (thanks to takePersistableUriPermission)
Follows Android guidelines (recommended since Android 5.0 / Lollipop)
Gives a consistent user experience — users don't have to guess which folder will work
Avoids many edge cases (some folders look writable but actually aren't)
Should you add a prompt / explanation for the user?
Yes — it's a good idea to guide the user, because many people don't know which folders are safe to pick.

Suggested UX improvements:

Before opening the picker, show a short explanation dialog:

text
To download books, please select a folder where Codex can save files.

Recommended locations:
• Documents
• Download
• A folder you created yourself (e.g. "My Books")

Important: Some cloud folders (Google Drive, OneDrive) or system-protected folders may not allow saving files.
After the user picks a folder, try to create a small test file (e.g. .nomedia or a dummy text file).
If it fails → show error message and ask them to pick another folder.

Example message:

text
Codex could not create files in the selected folder (read-only).

Please select a different folder, preferably in internal storage (Documents, Download, or a folder you created yourself).
Quick summary – what to do
Step	Action Needed	Why important
Request write + persistable flags	Yes – must add to intent	Prevents the "read-only collection" error
Take persistable permission	Yes – call takePersistableUriPermission	Lets app use folder after restart
Show explanation to user	Strongly recommended	Prevents users from picking bad folders
Test write access after selection	Recommended (optional but good UX)	Early feedback if user picked wrong folder
If you implement the intent flags + persistable permission correctly, the error should disappear for almost all users.


