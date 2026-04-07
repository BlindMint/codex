/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser.comic

import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import me.zhanghai.android.libarchive.Archive
import me.zhanghai.android.libarchive.ArchiveException
import me.zhanghai.android.libarchive.ArchiveEntry
import us.blindmint.codex.data.parser.NaturalOrderComparator
import us.blindmint.codex.domain.file.CachedFile
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.concurrent.Volatile
import javax.inject.Inject

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
        return when (format) {
            ArchiveFormat.ZIP, ArchiveFormat.RAR, ArchiveFormat.SEVEN_Z -> LibArchiveHandle(cachedFile)
            ArchiveFormat.UNKNOWN -> throw IllegalArgumentException("Unsupported archive format for file: ${cachedFile.name}")
        }
    }

    private class LibArchiveInputStream(buffer: Long, size: Long) : InputStream() {
        private val lock = Any()

        @Volatile
        private var isClosed = false

        private val archive = Archive.readNew()

        init {
            try {
                Archive.setCharset(archive, Charsets.UTF_8.name().toByteArray())
                Archive.readSupportFilterAll(archive)
                Archive.readSupportFormatAll(archive)
                Archive.readOpenMemoryUnsafe(archive, buffer, size)
            } catch (e: ArchiveException) {
                close()
                throw e
            }
        }

        private val oneByteBuffer = ByteBuffer.allocateDirect(1)

        override fun read(): Int {
            read(oneByteBuffer)
            return if (oneByteBuffer.hasRemaining()) oneByteBuffer.get().toUByte().toInt() else -1
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val buffer = ByteBuffer.wrap(b, off, len)
            read(buffer)
            return if (buffer.hasRemaining()) buffer.remaining() else -1
        }

        private fun read(buffer: ByteBuffer) {
            buffer.clear()
            Archive.readData(archive, buffer)
            buffer.flip()
        }

        override fun close() {
            synchronized(lock) {
                if (isClosed) return
                isClosed = true
            }
            Archive.readFree(archive)
        }

        fun getNextEntry(): LibArchiveEntryInfo? = Archive.readNextHeader(archive).takeUnless { it == 0L }?.let { entryPtr ->
            val name = ArchiveEntry.pathnameUtf8(entryPtr)
                ?: ArchiveEntry.pathname(entryPtr)?.decodeToString()
                ?: return null
            val isFile = ArchiveEntry.filetype(entryPtr) == ArchiveEntry.AE_IFREG
            LibArchiveEntryInfo(name, isFile)
        }
    }

    private data class LibArchiveEntryInfo(
        val name: String,
        val isFile: Boolean
    )

    private class LibArchiveHandle(private val cachedFile: CachedFile) : ArchiveHandle {
        private val size: Long
        private val address: Long

        private val _entries = mutableListOf<LibArchiveEntryImpl>()

        init {
            val file = cachedFile.rawFile
                ?: throw IllegalStateException("No raw file available for ${cachedFile.name}")

            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            size = pfd.statSize
            address = Os.mmap(0, size, OsConstants.PROT_READ, OsConstants.MAP_PRIVATE, pfd.fileDescriptor, 0)
            pfd.close()

            LibArchiveInputStream(address, size).use { archiveStream ->
                while (true) {
                    val entryInfo = archiveStream.getNextEntry() ?: break
                    if (entryInfo.isFile && ArchiveReader.isImageFile(entryInfo.name)) {
                        _entries.add(LibArchiveEntryImpl(entryInfo.name))
                    }
                }
            }

            _entries.sortWith(Comparator { a, b ->
                NaturalOrderComparator.compare(a.getPath(), b.getPath())
            })
        }

        override val entries: List<ComicArchiveEntry>
            get() = _entries

        override fun getInputStream(entry: ComicArchiveEntry): InputStream? {
            val libEntry = entry as LibArchiveEntryImpl
            val archive = LibArchiveInputStream(address, size)
            return try {
                while (true) {
                    val entryInfo = archive.getNextEntry() ?: break
                    if (entryInfo.name == libEntry.getPath()) {
                        val byteArray = archive.readBytes()
                        archive.close()
                        return byteArray.inputStream()
                    }
                }
                archive.close()
                null
            } catch (e: Exception) {
                archive.close()
                throw e
            }
        }

        override fun getEntryCount(): Int = _entries.size

        override fun close() {
            if (address != 0L) {
                Os.munmap(address, size)
            }
        }
    }

    private class LibArchiveEntryImpl(val name: String) : ComicArchiveEntry {
        override fun getPath(): String = name
        override fun getSize(): Long = 0
        override fun isDirectory(): Boolean = false
        override fun getMtime(): Long = 0
    }

    interface ArchiveHandle : AutoCloseable {
        val entries: List<ComicArchiveEntry>
        fun getInputStream(entry: ComicArchiveEntry): InputStream?
        fun getEntryCount(): Int
    }
}