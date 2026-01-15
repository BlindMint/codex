/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.room

import android.app.Application
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import us.blindmint.codex.data.local.dto.BookEntity
import us.blindmint.codex.data.local.dto.BookmarkEntity
import us.blindmint.codex.data.local.dto.BookProgressHistoryEntity
import us.blindmint.codex.data.local.dto.ColorPresetEntity
import us.blindmint.codex.data.local.dto.HistoryEntity
import us.blindmint.codex.data.local.dto.OpdsSourceEntity
import java.io.File

@Database(
    entities = [
        BookEntity::class,
        HistoryEntity::class,
        ColorPresetEntity::class,
        BookProgressHistoryEntity::class,
        BookmarkEntity::class,
        OpdsSourceEntity::class,
    ],
    version = 15,
    exportSchema = false
)
abstract class BookDatabase : RoomDatabase() {
    abstract val dao: BookDao
    abstract val opdsSourceDao: OpdsSourceDao
}

@Suppress("ClassName")
object DatabaseHelper {

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `LanguageHistoryEntity` (" +
                        "`languageCode` TEXT NOT NULL," +
                        " `order` INTEGER NOT NULL," +
                        " PRIMARY KEY(`languageCode`)" +
                        ")"
            )
        }
    }

    @DeleteColumn("BookEntity", "enableTranslator")
    @DeleteColumn("BookEntity", "translateFrom")
    @DeleteColumn("BookEntity", "translateTo")
    @DeleteColumn("BookEntity", "doubleClickTranslation")
    @DeleteColumn("BookEntity", "translateWhenOpen")
    @DeleteTable("LanguageHistoryEntity")
    class MIGRATION_3_4 : AutoMigrationSpec

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `ColorPresetEntity` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "`name` TEXT, " +
                        "`backgroundColor` INTEGER NOT NULL, " +
                        "`fontColor` INTEGER NOT NULL, " +
                        "`isSelected` INTEGER NOT NULL, " +
                        "`order` INTEGER NOT NULL" +
                        ")"
            )
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `FavoriteDirectoryEntity` (" +
                        "`path` TEXT NOT NULL, " +
                        "PRIMARY KEY(`path`)" +
                        ")"
            )
        }
    }

    @DeleteColumn("BookEntity", "textPath")
    @DeleteColumn("BookEntity", "chapters")
    class MIGRATION_7_8 : AutoMigrationSpec {
        companion object {
            /**
             * Along with textPath deletion,
             * books directory with text does not
             * serve any purpose.
             */
            fun removeBooksDir(application: Application) {
                val booksDir = File(application.filesDir, "books")

                if (booksDir.exists()) {
                    booksDir.deleteRecursively()
                }
            }
        }
    }

    @DeleteTable("FavoriteDirectoryEntity")
    class MIGRATION_8_9 : AutoMigrationSpec

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Migrate books with DROPPED category to PLANNING
            db.execSQL("UPDATE BookEntity SET category = 'PLANNING' WHERE category = 'DROPPED'")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `BookmarkEntity` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "`bookId` INTEGER NOT NULL, " +
                        "`scrollIndex` INTEGER NOT NULL, " +
                        "`scrollOffset` INTEGER NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL" +
                        ")"
            )
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `BookmarkEntity` ADD COLUMN `selectedText` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `BookmarkEntity` ADD COLUMN `customName` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `BookmarkEntity` ADD COLUMN `pageNumber` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `ColorPresetEntity` ADD COLUMN `isLocked` INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add OpdsSourceEntity table
            db.execSQL("""CREATE TABLE IF NOT EXISTS OpdsSourceEntity (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                url TEXT NOT NULL,
                username TEXT,
                password TEXT,
                enabled INTEGER NOT NULL DEFAULT 1,
                lastSync INTEGER NOT NULL DEFAULT 0
            )""")

            // Add OPDS and metadata fields to BookEntity
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `seriesName` TEXT")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `seriesIndex` INTEGER")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `publicationDate` INTEGER")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `language` TEXT")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `publisher` TEXT")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `summary` TEXT")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `uuid` TEXT")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `isbn` TEXT")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'LOCAL'")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `remoteUrl` TEXT")

            // Add comic-specific fields
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `isComic` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `pageCount` INTEGER")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `currentPage` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `lastPageRead` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `readingDirection` TEXT NOT NULL DEFAULT 'LTR'")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `comicReaderMode` TEXT NOT NULL DEFAULT 'PAGED'")
            db.execSQL("ALTER TABLE `BookEntity` ADD COLUMN `archiveFormat` TEXT")
        }
    }
}