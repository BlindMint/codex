package us.blindmint.codex.domain.use_case.book

import android.app.Application
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.data.parser.opf.OpfParser
import us.blindmint.codex.domain.file.CachedFile
import us.blindmint.codex.domain.file.CachedFileCompat
import us.blindmint.codex.domain.library.book.BookSource
import us.blindmint.codex.domain.opf.OpfMetadata
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.storage.CodexDirectoryManager
import us.blindmint.codex.presentation.core.constants.provideExtensions
import javax.inject.Inject

private const val BULK_IMPORT_CODEX = "BULK IMPORT CODEX"

data class BulkImportCodexProgress(
    val current: Int,
    val total: Int,
    val currentFile: String
)

class BulkImportCodexDirectoryUseCase @Inject constructor(
    private val application: Application,
    private val codexDirectoryManager: CodexDirectoryManager,
    private val fileParser: FileParser,
    private val opfParser: OpfParser,
    private val bookRepository: BookRepository,
    private val insertBook: InsertBook
) {

    suspend fun execute(
        onProgress: (BulkImportCodexProgress) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        Log.i(BULK_IMPORT_CODEX, "Starting bulk import from Codex Directory downloads folder")

        if (!codexDirectoryManager.isConfigured()) {
            Log.w(BULK_IMPORT_CODEX, "Codex directory not configured, skipping import")
            return@withContext 0
        }

        val downloadsDir = codexDirectoryManager.getDownloadsDir()
        if (downloadsDir == null) {
            Log.w(BULK_IMPORT_CODEX, "Downloads directory not available")
            return@withContext 0
        }

        Log.i(BULK_IMPORT_CODEX, "Downloads directory: ${downloadsDir.uri}")
        val supportedExtensions = provideExtensions()
        val existingPaths = bookRepository.getBooks("").map { it.filePath }
        Log.i(BULK_IMPORT_CODEX, "Found ${existingPaths.size} existing books")

        var importedCount = 0

        val authorFolders = try {
            downloadsDir.listFiles()?.filter { it.isDirectory }?.toTypedArray()
        } catch (e: Exception) {
            Log.e(BULK_IMPORT_CODEX, "Failed to list author folders", e)
            return@withContext 0
        } ?: emptyArray()

        Log.i(BULK_IMPORT_CODEX, "Found ${authorFolders.size} author folders")

        val allBookFolders = mutableListOf<DocumentFile>()

        authorFolders.forEach { authorFolder ->
            val bookFolders = try {
                authorFolder.listFiles()?.filter { it.isDirectory } ?: emptyList()
            } catch (e: Exception) {
                Log.e(BULK_IMPORT_CODEX, "Failed to list books in ${authorFolder.name}", e)
                emptyList()
            }

            allBookFolders.addAll(bookFolders)
        }

        Log.i(BULK_IMPORT_CODEX, "Total of ${allBookFolders.size} book folders to process")

        if (allBookFolders.isEmpty()) {
            Log.i(BULK_IMPORT_CODEX, "No book folders found")
            return@withContext 0
        }

        allBookFolders.forEachIndexed { index, folder ->
            val result = processBookFolder(
                folder = folder,
                supportedExtensions = supportedExtensions,
                existingPaths = existingPaths,
                onProgress = {
                    onProgress(BulkImportCodexProgress(index + 1, allBookFolders.size, it))
                }
            )

            if (result != null) {
                importedCount++
                Log.i(BULK_IMPORT_CODEX, "Imported: ${folder.name}")
            } else {
                Log.d(BULK_IMPORT_CODEX, "No book imported from: ${folder.name}")
            }
        }

        Log.i(BULK_IMPORT_CODEX, "Bulk import completed. Imported $importedCount books.")
        importedCount
    }

    private suspend fun processBookFolder(
        folder: DocumentFile,
        supportedExtensions: List<String>,
        existingPaths: List<String>,
        onProgress: (String) -> Unit
    ): us.blindmint.codex.domain.library.book.BookWithCover? {
        Log.d(BULK_IMPORT_CODEX, "Processing folder: ${folder.name}")

        val folderFiles = folder.listFiles()
        if (folderFiles.isNullOrEmpty()) {
            Log.d(BULK_IMPORT_CODEX, "Folder is empty: ${folder.name}")
            return null
        }

        val opfFile = folderFiles.firstOrNull { it.name?.endsWith(".opf", ignoreCase = true) == true }

        val bookFiles = folderFiles.filter { file ->
            file.isFile && supportedExtensions.any { ext ->
                file.name?.endsWith(ext, ignoreCase = true) == true
            }
        }

        if (bookFiles.isEmpty()) {
            Log.d(BULK_IMPORT_CODEX, "No book files in: ${folder.name}")
            return null
        }

        val bookFile = bookFiles.first()
        val bookUriString = bookFile.uri.toString()

        val alreadyExists = existingPaths.any { existingPath ->
            existingPath.equals(bookUriString, ignoreCase = true)
        }

        if (alreadyExists) {
            Log.d(BULK_IMPORT_CODEX, "Book already exists: ${bookFile.name}")
            return null
        }

        onProgress(folder.name ?: "Unknown")

        return try {
            val cachedFile = CachedFile(
                context = application,
                uri = bookFile.uri,
                builder = CachedFileCompat.build(
                    name = bookFile.name ?: "unknown",
                    path = bookFile.uri.toString(),
                    isDirectory = false
                )
            )

            val parsedBook = fileParser.parse(cachedFile)
            if (parsedBook == null) {
                Log.w(BULK_IMPORT_CODEX, "Failed to parse: ${bookFile.name}")
                return null
            }

            var finalBook = parsedBook.book

            if (opfFile != null) {
                try {
                    val opfMetadata = opfParser.parse(opfFile)
                    if (opfMetadata != null) {
                        finalBook = mergeOpfMetadata(finalBook, opfMetadata)
                    }
                } catch (e: Exception) {
                    Log.w(BULK_IMPORT_CODEX, "Failed to parse OPF: ${opfFile.name}", e)
                }
            }

            finalBook = finalBook.copy(
                source = BookSource.OPDS,
                category = us.blindmint.codex.domain.library.category.Category.READING
            )

            val bookWithCover = us.blindmint.codex.domain.library.book.BookWithCover(
                book = finalBook,
                coverImage = parsedBook.coverImage
            )

            insertBook.execute(bookWithCover)
            bookWithCover
        } catch (e: Exception) {
            Log.e(BULK_IMPORT_CODEX, "Error processing ${bookFile.name}", e)
            null
        }
    }

    private fun mergeOpfMetadata(
        book: us.blindmint.codex.domain.library.book.Book,
        opfMetadata: OpfMetadata
    ): us.blindmint.codex.domain.library.book.Book {
        return book.copy(
            title = opfMetadata.title ?: book.title,
            authors = (book.authors + listOfNotNull(opfMetadata.author)).distinct()
                .takeIf { it.isNotEmpty() } ?: book.authors,
            description = opfMetadata.description ?: book.description,
            tags = opfMetadata.tags.takeIf { it.isNotEmpty() } ?: book.tags,
            series = (book.series + listOfNotNull(opfMetadata.series)).distinct()
                .takeIf { it.isNotEmpty() } ?: book.series,
            languages = (book.languages + listOfNotNull(opfMetadata.language)).distinct()
                .takeIf { it.isNotEmpty() } ?: book.languages,
            publicationDate = opfMetadata.publicationDate ?: book.publicationDate,
            publisher = opfMetadata.publisher ?: book.publisher,
            isbn = opfMetadata.isbn ?: book.isbn
        )
    }
}
