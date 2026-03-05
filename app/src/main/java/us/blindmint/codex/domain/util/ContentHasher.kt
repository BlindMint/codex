/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.domain.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.jpountz.xxhash.XXHashFactory
import java.io.IOException
import java.io.InputStream

object ContentHasher {
    private val hashFactory = XXHashFactory.fastestInstance()
    private const val SEED: Long = 0x9747B28CL
    private const val BUFFER_SIZE = 8192
    private const val PARTIAL_HASH_LIMIT = 65536 // 64 KB

    suspend fun computeHash(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            computeHashFromStream(input)
        } ?: throw IOException("Cannot open file: $uri")
    }

    /**
     * Reads only the first 64 KB of the file to produce a fast approximate hash.
     * Sufficient for duplicate detection during bulk import — files sharing the
     * same first 64 KB are overwhelmingly the same content.
     */
    suspend fun computePartialHash(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val hasher = hashFactory.newStreamingHash64(SEED)
            val buffer = ByteArray(BUFFER_SIZE)
            var remaining = PARTIAL_HASH_LIMIT
            var bytesRead = 0
            while (remaining > 0 && input.read(buffer, 0, minOf(BUFFER_SIZE, remaining)).also { bytesRead = it } != -1) {
                hasher.update(buffer, 0, bytesRead)
                remaining -= bytesRead
            }
            hasher.value.toString(16).padStart(16, '0')
        } ?: throw IOException("Cannot open file: $uri")
    }

    fun computeHashFromStream(input: InputStream): String {
        val hasher = hashFactory.newStreamingHash64(SEED)
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            hasher.update(buffer, 0, bytesRead)
        }
        val hashValue = hasher.value
        return hashValue.toString(16).padStart(16, '0')
    }

    suspend fun computeHashFromBytes(data: ByteArray): String = withContext(Dispatchers.IO) {
        val hasher = hashFactory.hash64()
        val hashValue = hasher.hash(data, 0, data.size, SEED)
        hashValue.toString(16).padStart(16, '0')
    }
}
