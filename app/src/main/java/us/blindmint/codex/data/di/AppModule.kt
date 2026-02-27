/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.di

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.commonmark.node.BlockQuote
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import us.blindmint.codex.data.local.room.BookDao
import us.blindmint.codex.data.local.room.BookDatabase
import us.blindmint.codex.data.local.room.DatabaseHelper
import us.blindmint.codex.domain.use_case.book.BulkImportBooksFromFolder
import us.blindmint.codex.domain.use_case.book.BulkImportCodexDirectoryUseCase
import us.blindmint.codex.ui.import_progress.ImportProgressService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCommonmarkParser(): Parser {
        return Parser
            .builder()
            .enabledBlockTypes(
                setOf(
                    Heading::class.java,
                    HtmlBlock::class.java,
                    ThematicBreak::class.java,
                    BlockQuote::class.java,
                    FencedCodeBlock::class.java,
                    IndentedCodeBlock::class.java,
                    ThematicBreak::class.java
                )
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideBookDao(app: Application): BookDao {
        // Additional Migrations
        DatabaseHelper.MIGRATION_7_8.removeBooksDir(app)

        // For now, disable encryption to avoid 16KB page size issues and migration problems
        // TODO: Implement proper database encryption migration for production
        return Room.databaseBuilder(
            app,
            BookDatabase::class.java,
            "book_db"
        )
            .addMigrations(
                DatabaseHelper.MIGRATION_2_3, // creates LanguageHistoryEntity table(if does not exist)
                DatabaseHelper.MIGRATION_4_5, // creates ColorPresetEntity table(if does not exist)
                DatabaseHelper.MIGRATION_5_6, // creates FavoriteDirectoryEntity table(if does not exist)
                DatabaseHelper.MIGRATION_10_11, // migrates DROPPED category to PLANNING
                DatabaseHelper.MIGRATION_11_12, // creates BookmarkEntity table
                DatabaseHelper.MIGRATION_12_13, // adds selectedText, customName, pageNumber to BookmarkEntity
                DatabaseHelper.MIGRATION_13_14, // adds isLocked to ColorPresetEntity
                DatabaseHelper.MIGRATION_17_18, // adds speedReaderWordIndex and speedReaderHasBeenOpened
                DatabaseHelper.MIGRATION_18_19, // adds totalWordCount
                DatabaseHelper.MIGRATION_19_20, // removes totalWordCount, summary, remoteUrl, archiveFormat
                DatabaseHelper.MIGRATION_20_21, // adds speedReaderTotalWords
                DatabaseHelper.MIGRATION_21_22, // adds usernameEncrypted and passwordEncrypted to OpdsSourceEntity
                DatabaseHelper.MIGRATION_22_23,
                DatabaseHelper.MIGRATION_23_24,
                DatabaseHelper.MIGRATION_24_25,
                DatabaseHelper.MIGRATION_25_26 // fixes isComic field for existing comic books
            )
            .allowMainThreadQueries()
            .build()
            .dao
    }

    @Provides
    @Singleton
    fun provideImportProgressService(
        bulkImportBooksFromFolder: BulkImportBooksFromFolder,
        bulkImportCodexDirectoryUseCase: BulkImportCodexDirectoryUseCase
    ): ImportProgressService {
        return ImportProgressService(bulkImportBooksFromFolder, bulkImportCodexDirectoryUseCase)
    }
}