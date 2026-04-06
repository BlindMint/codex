/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.comic

import android.util.Log
import us.blindmint.codex.data.parser.NaturalOrderComparator
import us.blindmint.codex.domain.file.CachedFile
import java.io.File
import java.io.InputStream
import javax.inject.Inject

private const val TAG = "ArchiveReader"
private const val CBR_DEBUG_TAG = "CodexComicSlider"

class ArchiveReader @Inject constructor() {

    companion object {
        internal fun isImageFile(path: String): Boolean {
            val extension = path.substringAfterLast('.', "").lowercase()
            return extension in setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
        }
    }

    enum class ArchiveFormat {
        ZIP, RAR, SEVEN_Z, UNKNOWN
    }

    fun getArchiveFormat(cachedFile: CachedFile): ArchiveFormat {
        return when (cachedFile.name.substringAfterLast('.').lowercase()) {
            "zip", "cbz" -> ArchiveFormat.ZIP
            "rar", "cbr" -> ArchiveFormat.RAR
            "7z", "cb7" -> ArchiveFormat.SEVEN_Z
            else -> ArchiveFormat.UNKNOWN
        }
    }

    fun openArchive(cachedFile: CachedFile): ArchiveHandle {
        val format = getArchiveFormat(cachedFile)
        android.util.Log.d("CodexComic", "Opening archive ${cachedFile.name} with format: $format")
        return when (format) {
            ArchiveFormat.ZIP -> LibArchiveHandle(cachedFile)
            ArchiveFormat.RAR -> RarArchiveHandle(cachedFile)
            ArchiveFormat.SEVEN_Z -> SevenZArchiveHandle(cachedFile)
            ArchiveFormat.UNKNOWN -> throw IllegalArgumentException("Unsupported archive format for file: ${cachedFile.name}")
        }
    }

    private class SevenZArchiveHandle(private val cachedFile: CachedFile) : ArchiveHandle {
        private val _entries = mutableListOf<SevenZArchiveEntry>()
        private var sevenZFile: org.apache.commons.compress.archivers.sevenz.SevenZFile? = null
        private var tempFile: java.io.File? = null

        init {
            loadArchive()
        }

        @Suppress("DEPRECATION") // SevenZFile(File) constructor is deprecated but still required
        private fun loadArchive() {
            try {
                android.util.Log.d("SevenZArchiveHandle", "Loading 7Z archive: ${cachedFile.name}")

                // Use cached file if available (preferred for content:// URIs)
                cachedFile.rawFile?.let { cachedRawFile ->
                    android.util.Log.d("SevenZArchiveHandle", "Using raw file: ${cachedRawFile.absolutePath}")
                    sevenZFile = org.apache.commons.compress.archivers.sevenz.SevenZFile(cachedRawFile)
                } ?: run {
                    // For content:// URIs, copy to temp file first
                    val temp = createTempFileFromUri()
                    if (temp != null) {
                        android.util.Log.d("SevenZArchiveHandle", "Using temp file: ${temp.absolutePath}")
                        sevenZFile = org.apache.commons.compress.archivers.sevenz.SevenZFile(temp)
                        tempFile = temp
                    } else {
                        throw Exception("Failed to create temp file from URI")
                    }
                }

// Read all entries
                sevenZFile?.use { szf ->
                    for (entry in szf.entries) {
                        if (!entry.isDirectory && ArchiveReader.isImageFile(entry.name)) {
                            _entries.add(SevenZArchiveEntry(entry))
                        }
                    }
                }

                // Sort entries by name using natural ordering (alphanumeric sorting)
                // to ensure correct page sequence (e.g., page1 < page2 < page10)
                _entries.sortWith(Comparator { a, b ->
                    NaturalOrderComparator.compare(a.entry.name, b.entry.name)
                })

                android.util.Log.d("SevenZArchiveHandle", "Found ${_entries.size} image entries")

                // Re-open for reading (SevenZFile can only be iterated once)
                cachedFile.rawFile?.let { cachedRawFile ->
                    sevenZFile = org.apache.commons.compress.archivers.sevenz.SevenZFile(cachedRawFile)
                } ?: tempFile?.let { temp ->
                    sevenZFile = org.apache.commons.compress.archivers.sevenz.SevenZFile(temp)
                }

            } catch (e: Exception) {
                android.util.Log.e("SevenZArchiveHandle", "Failed to load 7Z archive", e)
                throw e
            }
        }

        private fun createTempFileFromUri(): java.io.File? {
            return try {
                val tempFile = java.io.File.createTempFile("temp_archive", ".7z")
                tempFile.deleteOnExit()

                cachedFile.openInputStream()?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } catch (e: Exception) {
                android.util.Log.e("SevenZArchiveHandle", "Failed to create temp file", e)
                null
            }
        }

        override val entries: List<ArchiveEntry>
            get() = _entries

        override fun getInputStream(entry: ArchiveEntry): InputStream {
            val szEntry = (entry as SevenZArchiveEntry).entry
            sevenZFile?.let { szf ->
                // Find and return the entry's input stream
                szf.entries.forEach { currentEntry ->
                    if (currentEntry.name == szEntry.name) {
                        return szf.getInputStream(currentEntry)
                    }
                }
            }
            throw IllegalStateException("Archive not open or entry not found")
        }

        override fun getEntryCount(): Int = entries.size

        override fun close() {
            sevenZFile?.close()
            sevenZFile = null
            tempFile?.delete()
            tempFile = null
        }
    }

    private class SevenZArchiveEntry(val entry: org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry) : ArchiveEntry {
        override fun getPath(): String = entry.name
        override fun getSize(): Long = entry.size
        override fun isDirectory(): Boolean = entry.isDirectory
        override fun getMtime(): Long = entry.lastModifiedDate?.time ?: 0L
    }

    interface ArchiveHandle : AutoCloseable {
        val entries: List<ArchiveEntry>
        fun getInputStream(entry: ArchiveEntry): InputStream
        fun getEntryCount(): Int
    }

    private class LibArchiveHandle(private val cachedFile: CachedFile) : ArchiveHandle {
        private val _entries = mutableListOf<LibArchiveEntry>()
        private var zipFile: java.util.zip.ZipFile? = null
        private var tempFile: java.io.File? = null

        init {
            loadArchive()
        }

        private fun loadArchive() {
            try {
                android.util.Log.d("CodexComic", "Loading archive: ${cachedFile.name}")

                // Use cached file if available (preferred for content:// URIs)
                cachedFile.rawFile?.let { cachedRawFile ->
                    android.util.Log.d("CodexComic", "Using raw file: ${cachedRawFile.absolutePath}")
                    zipFile = java.util.zip.ZipFile(cachedRawFile)
                } ?: run {
                    // Fallback: Use Java's built-in ZipFile for CBZ files
                    // For content:// URIs, we need to copy to temp file first
                    if (cachedFile.uri.scheme == "file") {
                        try {
                            zipFile = java.util.zip.ZipFile(java.io.File(cachedFile.path))
                        } catch (e: java.io.FileNotFoundException) {
                            // If direct access fails, try to copy via InputStream
                            Log.w(TAG, "Direct file access failed, trying InputStream copy for: ${cachedFile.path}")
                            tempFile = createTempFileFromInputStream()
                            if (tempFile != null) {
                                zipFile = java.util.zip.ZipFile(tempFile)
                            } else {
                                throw Exception("Failed to access file: both direct access and InputStream copy failed")
                            }
                        }
                    } else {
                        // For content:// URIs, copy to temp file first
                        tempFile = createTempFileFromUri()
                        if (tempFile != null) {
                            zipFile = java.util.zip.ZipFile(tempFile)
                        } else {
                            throw Exception("Failed to create temp file from URI")
                        }
                    }
                }

                // Read all entries (must happen for ALL code paths)
                val entries = zipFile!!.entries()
                var entryCount = 0
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && ArchiveReader.isImageFile(entry.name)) {
                        _entries.add(LibArchiveEntry(ZipEntryAdapter(entry)))
                        entryCount++
                    }
                }
                android.util.Log.d("CodexComic", "Found $entryCount image entries")

                // Sort entries by name using natural ordering (alphanumeric sorting)
                // to ensure correct page sequence (e.g., page1 < page2 < page10)
                _entries.sortWith(Comparator { a, b ->
                    NaturalOrderComparator.compare(a.entry.getPath(), b.entry.getPath())
                })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load archive: ${cachedFile.path}", e)
                throw e
            }
        }

        private fun createTempFileFromInputStream(): java.io.File? {
            return try {
                cachedFile.openInputStream()?.use { input ->
                    val tempFile = java.io.File.createTempFile("temp_archive", ".zip")
                    tempFile.deleteOnExit()

                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    tempFile
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create temp file from InputStream", e)
                null
            }
        }

        private fun createTempFileFromUri(): java.io.File? {
            return try {
                val tempFile = java.io.File.createTempFile("temp_archive", ".zip")
                tempFile.deleteOnExit()

                cachedFile.openInputStream()?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create temp file", e)
                null
            }
        }

        override val entries: List<ArchiveEntry>
            get() = _entries

        override fun getInputStream(entry: ArchiveEntry): InputStream {
            val zipEntry = (entry as LibArchiveEntry).entry as ZipEntryAdapter
            zipFile?.let { zip ->
                return zip.getInputStream(zipEntry.zipEntry)
            }
            throw IllegalStateException("Archive not open")
        }

        override fun getEntryCount(): Int = entries.size

        override fun close() {
            zipFile?.close()
            zipFile = null
            tempFile?.delete()
            tempFile = null
        }
    }

    private class RarArchiveHandle(private val cachedFile: CachedFile) : ArchiveHandle {
        private val _entries = mutableListOf<RarArchiveEntry>()
        private var archive: com.github.junrar.Archive? = null
        private var tempFile: java.io.File? = null

        init {
            loadArchive()
        }

        private fun loadArchive() {
            try {
                // Use cached file if available (preferred for content:// URIs)
                cachedFile.rawFile?.let { cachedRawFile ->
                    archive = com.github.junrar.Archive(cachedRawFile)
                    android.util.Log.d(CBR_DEBUG_TAG, "[RarArchiveHandle] Opened archive from rawFile: ${cachedRawFile.absolutePath}")
                } ?: run {
                    // Fallback: Try to access the file directly first (for file:// URIs)
                    if (cachedFile.uri.scheme == "file") {
                        try {
                            archive = com.github.junrar.Archive(java.io.File(cachedFile.path))
                            android.util.Log.d(CBR_DEBUG_TAG, "[RarArchiveHandle] Opened archive from path: ${cachedFile.path}")
                        } catch (e: java.io.FileNotFoundException) {
                            // If direct file access fails, try to copy via InputStream
                            Log.w(TAG, "Direct file access failed, trying InputStream copy for: ${cachedFile.path}")
                            tempFile = createTempFileFromInputStream()
                            if (tempFile != null) {
                                archive = com.github.junrar.Archive(tempFile)
                                android.util.Log.d(CBR_DEBUG_TAG, "[RarArchiveHandle] Opened archive from tempFile: ${tempFile!!.absolutePath}")
                            } else {
                                throw Exception("Failed to access file: both direct access and InputStream copy failed")
                            }
                        }
                    } else {
                        // For content:// URIs, copy to temp file first
                        tempFile = createTempFileFromUri()
                        if (tempFile != null) {
                            archive = com.github.junrar.Archive(tempFile)
                            android.util.Log.d(CBR_DEBUG_TAG, "[RarArchiveHandle] Opened archive from URI tempFile: ${tempFile!!.absolutePath}")
                        } else {
                            throw Exception("Failed to create temp file from URI")
                        }
                    }
                }

                // Read all file headers (must happen for ALL code paths)
                for (header in archive!!.fileHeaders) {
                    if (!header.isDirectory && ArchiveReader.isImageFile(header.fileName)) {
                        _entries.add(RarArchiveEntry(header))
                    }
                }

                android.util.Log.d(CBR_DEBUG_TAG, "[RarArchiveHandle] Total entries found: ${_entries.size}")
                for (i in _entries.indices) {
                    android.util.Log.d(CBR_DEBUG_TAG, "[RarArchiveHandle] Entry $i: ${_entries[i].header.fileName}")
                }

                // Sort entries by filename using natural ordering (alphanumeric sorting)
                // to ensure correct page sequence (e.g., page1 < page2 < page10)
                _entries.sortWith(Comparator { a, b ->
                    NaturalOrderComparator.compare(a.header.fileName, b.header.fileName)
                })

                android.util.Log.d(CBR_DEBUG_TAG, "[RarArchiveHandle] After sorting:")
                for (i in _entries.indices) {
                    android.util.Log.d(CBR_DEBUG_TAG, "[RarArchiveHandle] Entry $i: ${_entries[i].header.fileName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load RAR archive: ${cachedFile.path}", e)
                throw e
            }
        }

        private fun createTempFileFromInputStream(): java.io.File? {
            return try {
                cachedFile.openInputStream()?.use { input ->
                    val tempFile = java.io.File.createTempFile("temp_archive", ".rar")
                    tempFile.deleteOnExit()

                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    tempFile
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create temp file from InputStream", e)
                null
            }
        }

        private fun createTempFileFromUri(): java.io.File? {
            return try {
                val tempFile = java.io.File.createTempFile("temp_archive", ".rar")
                tempFile.deleteOnExit()

                cachedFile.openInputStream()?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create temp file", e)
                null
            }
        }

        override val entries: List<ArchiveEntry>
            get() = _entries

        override fun getInputStream(entry: ArchiveEntry): InputStream {
            val rarHeader = (entry as RarArchiveEntry).header
            archive?.let { arch ->
                android.util.Log.d(CBR_DEBUG_TAG, "[RarArchiveHandle] getInputStream called for: ${rarHeader.fileName}")
                return arch.getInputStream(rarHeader)
            }
            throw IllegalStateException("Archive not open")
        }

        override fun getEntryCount(): Int = entries.size

        override fun close() {
            archive?.close()
            archive = null
            tempFile?.delete()
            tempFile = null
        }
    }

    private class LibArchiveEntry(val entry: ArchiveEntry) : ArchiveEntry {
        override fun getPath(): String = entry.getPath()
        override fun getSize(): Long = entry.getSize()
        override fun isDirectory(): Boolean = entry.isDirectory()
        override fun getMtime(): Long = entry.getMtime()
    }

    private class RarArchiveEntry(val header: com.github.junrar.rarfile.FileHeader) : ArchiveEntry {
        override fun getPath(): String = header.fileName
        override fun getSize(): Long = header.fullUnpackSize.toLong()
        override fun isDirectory(): Boolean = header.isDirectory
        override fun getMtime(): Long = header.mTime?.time ?: 0L
    }

    private class ZipEntryAdapter(val zipEntry: java.util.zip.ZipEntry) : ArchiveEntry {
        override fun getPath(): String = zipEntry.name
        override fun getSize(): Long = zipEntry.size
        override fun isDirectory(): Boolean = zipEntry.isDirectory
        override fun getMtime(): Long = zipEntry.time
    }
}