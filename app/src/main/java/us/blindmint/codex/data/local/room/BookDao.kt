/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import us.blindmint.codex.data.local.dto.BookEntity
import us.blindmint.codex.data.local.dto.BookmarkEntity
import us.blindmint.codex.data.local.dto.BookProgressHistoryEntity
import us.blindmint.codex.data.local.dto.ColorPresetEntity
import us.blindmint.codex.data.local.dto.HistoryEntity

/**
 * Class to manipulate Room database.
 */
@Dao
interface BookDao {

    /* ------ BookEntity ------------------------ */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(
        book: BookEntity
    )

    @Query(
        """
        SELECT * FROM bookentity
        WHERE LOWER(title) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(COALESCE(author, '')) LIKE '%' || LOWER(:query) || '%'
           OR LOWER(COALESCE(description, '')) LIKE '%' || LOWER(:query) || '%'
    """
    )
    suspend fun searchBooks(query: String): List<BookEntity>

    @Query("SELECT * FROM bookentity WHERE id=:id")
    suspend fun findBookById(id: Int): BookEntity

    @Query("SELECT * FROM bookentity WHERE id IN (:ids)")
    suspend fun findBooksById(ids: List<Int>): List<BookEntity>

    @Query("SELECT * FROM bookentity WHERE filePath=:filePath LIMIT 1")
    suspend fun findBookByFilePath(filePath: String): BookEntity?

    @Delete
    suspend fun deleteBooks(books: List<BookEntity>)

    @Update
    suspend fun updateBooks(books: List<BookEntity>)
    /* - - - - - - - - - - - - - - - - - - - - - - */


    /* ------ HistoryEntity --------------------- */
    @Query("SELECT * FROM historyentity")
    suspend fun getHistory(): List<HistoryEntity>

    @Query("SELECT * FROM historyentity WHERE bookId = :bookId ORDER BY time DESC LIMIT 1")
    fun getLatestHistoryForBook(bookId: Int): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(
        history: List<HistoryEntity>
    )

    @Query("DELETE FROM historyentity")
    suspend fun deleteWholeHistory()

    @Query("DELETE FROM historyentity WHERE bookId = :bookId")
    suspend fun deleteBookHistory(bookId: Int)

    @Delete
    suspend fun deleteHistory(history: List<HistoryEntity>)
    /* - - - - - - - - - - - - - - - - - - - - - - */


    /* ------ BookProgressHistoryEntity -------- */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookProgressHistory(history: BookProgressHistoryEntity)

    @Query("SELECT * FROM bookprogresshistoryentity WHERE filePath = :filePath")
    suspend fun getBookProgressHistory(filePath: String): BookProgressHistoryEntity?

    @Query("DELETE FROM bookprogresshistoryentity WHERE filePath = :filePath")
    suspend fun deleteBookProgressHistory(filePath: String)

    @Query("DELETE FROM bookprogresshistoryentity WHERE lastModified < :olderThan")
    suspend fun deleteOldProgressHistory(olderThan: Long): Int

    @Query("SELECT COUNT(*) FROM bookprogresshistoryentity")
    suspend fun getProgressHistoryCount(): Int
    /* - - - - - - - - - - - - - - - - - - - - - - */


    /* ------ ColorPresetEntity ----------------- */
    @Upsert
    suspend fun updateColorPreset(colorPreset: ColorPresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColorPreset(colorPreset: ColorPresetEntity)

    @Query("SELECT `order` FROM colorpresetentity WHERE :id=id")
    suspend fun getColorPresetOrder(id: Int): Int?

    @Query("SELECT COUNT(*) FROM colorpresetentity")
    suspend fun getColorPresetsSize(): Int

    @Query("SELECT * FROM colorpresetentity")
    suspend fun getColorPresets(): List<ColorPresetEntity>

    @Delete
    suspend fun deleteColorPreset(colorPreset: ColorPresetEntity)

    @Query("DELETE FROM colorpresetentity")
    suspend fun deleteColorPresets()
    /* - - - - - - - - - - - - - - - - - - - - - - */


    /* ------ BookmarkEntity ------------------- */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarkentity WHERE bookId = :bookId ORDER BY timestamp DESC")
    suspend fun getBookmarksByBookId(bookId: Int): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarkentity WHERE id = :bookmarkId")
    suspend fun getBookmarkById(bookmarkId: Int): BookmarkEntity?

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarkentity WHERE bookId = :bookId")
    suspend fun deleteBookmarksByBookId(bookId: Int)

    @Query("DELETE FROM bookmarkentity")
    suspend fun deleteAllBookmarks()
    /* - - - - - - - - - - - - - - - - - - - - - - */
}