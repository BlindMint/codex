/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.blindmint.codex.domain.use_case.book

import ua.blindmint.codex.domain.library.book.Book
import ua.blindmint.codex.domain.library.book.BookSyncData
import ua.blindmint.codex.domain.repository.FileSystemRepository
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class SyncProgressUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {

    private val defaultSyncDir = "/sdcard/CodexSync"

    suspend fun saveProgress(book: Book, sharedDir: String = defaultSyncDir) {
        val file = File(book.filePath)
        if (!file.exists()) return

        val fileSize = file.length()
        val fileHash = calculateFileHash(file)
        val syncData = BookSyncData(
            title = book.title.getAsString(),
            author = book.author.getAsString(),
            fileSize = fileSize,
            fileHash = fileHash,
            progress = book.progress,
            lastUpdated = java.time.Instant.now().toString()
        )

        val json = Json.encodeToString(syncData)
        val syncFile = File(sharedDir, "${book.id}.json")
        syncFile.writeText(json)
    }

    suspend fun loadProgress(book: Book, sharedDir: String = defaultSyncDir): Float? {
        val file = File(book.filePath)
        if (!file.exists()) return null

        val fileSize = file.length()
        val fileHash = calculateFileHash(file)
        val syncFile = File(sharedDir, "${book.id}.json")

        if (!syncFile.exists()) return null

        return try {
            val json = syncFile.readText()
            val syncData = Json.decodeFromString<BookSyncData>(json)
            if (syncData.title == book.title.getAsString() &&
                syncData.author == book.author.getAsString() &&
                syncData.fileSize == fileSize &&
                syncData.fileHash == fileHash) {
                syncData.progress
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateFileHash(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}