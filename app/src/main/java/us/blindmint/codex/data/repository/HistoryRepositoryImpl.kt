/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import us.blindmint.codex.data.local.room.BookDao
import us.blindmint.codex.data.local.dto.HistoryEntity
import us.blindmint.codex.data.mapper.history.HistoryMapper
import us.blindmint.codex.domain.history.History
import us.blindmint.codex.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * History repository.
 * Manages all [History] related work.
 */
@Singleton
class HistoryRepositoryImpl @Inject constructor(
    database: BookDao,
    private val historyMapper: HistoryMapper
) : BaseRepository<History, HistoryEntity, BookDao>(), HistoryRepository {

    override val dao = database

    override suspend fun insertHistory(history: History) {
        dao.insertHistory(listOf(historyMapper.toHistoryEntity(history)))
    }

    override suspend fun getHistory(): List<History> {
        return dao.getHistory().map { historyMapper.toHistory(it) }
    }

    override suspend fun getLatestBookHistory(bookId: Int): History? {
        return dao.getLatestHistoryForBook(bookId)?.let { historyMapper.toHistory(it) }
    }

    override suspend fun deleteWholeHistory() {
        dao.deleteWholeHistory()
    }

    override suspend fun deleteBookHistory(bookId: Int) {
        dao.deleteBookHistory(bookId)
    }

    override suspend fun deleteHistory(history: History) {
        dao.deleteHistory(listOf(historyMapper.toHistoryEntity(history)))
    }
}
