/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.comic

import android.util.Log
import us.blindmint.codex.domain.file.CachedFile
import java.io.File
import java.io.InputStream
import javax.inject.Inject

private const val TAG = "ArchiveReader"

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
        android.util.Log.d("ArchiveReader", "Opening archive ${cachedFile.name} with format: $format")
        return when (format) {
            ArchiveFormat.ZIP, ArchiveFormat.SEVEN_Z -> LibArchiveHandle(cachedFile)
            ArchiveFormat.RAR -> RarArchiveHandle(cachedFile)
            ArchiveFormat.UNKNOWN -> throw IllegalArgumentException("Unsupported archive format for file: ${cachedFile.name}")
        }
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
                android.util.Log.d("LibArchiveHandle", "Loading archive: ${cachedFile.name}")

                // Use cached file if available (preferred for content:// URIs)
                cachedFile.rawFile?.let { cachedRawFile ->
                    android.util.Log.d("LibArchiveHandle", "Using raw file: ${cachedRawFile.absolutePath}")
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
                android.util.Log.d("LibArchiveHandle", "Reading entries from zip file")
                val entries = zipFile!!.entries()
                var entryCount = 0
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    android.util.Log.d("LibArchiveHandle", "Entry: ${entry.name}, isDir: ${entry.isDirectory}")
                    if (!entry.isDirectory && ArchiveReader.isImageFile(entry.name)) {
                        _entries.add(LibArchiveEntry(ZipEntryAdapter(entry)))
                        entryCount++
                    }
                }
                android.util.Log.d("LibArchiveHandle", "Found $entryCount image entries")

                // Sort entries by name for consistent ordering
                _entries.sortBy { it.entry.getPath() }
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
                } ?: run {
                    // Fallback: Try to access the file directly first (for file:// URIs)
                    if (cachedFile.uri.scheme == "file") {
                        try {
                            archive = com.github.junrar.Archive(java.io.File(cachedFile.path))
                        } catch (e: java.io.FileNotFoundException) {
                            // If direct access fails, try to copy via InputStream
                            Log.w(TAG, "Direct file access failed, trying InputStream copy for: ${cachedFile.path}")
                            tempFile = createTempFileFromInputStream()
                            if (tempFile != null) {
                                archive = com.github.junrar.Archive(tempFile)
                            } else {
                                throw Exception("Failed to access file: both direct access and InputStream copy failed")
                            }
                        }
                    } else {
                        // For content:// URIs, copy to temp file first
                        tempFile = createTempFileFromUri()
                        if (tempFile != null) {
                            archive = com.github.junrar.Archive(tempFile)
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

                // Sort entries by filename for consistent ordering
                _entries.sortBy { it.header.fileName }
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