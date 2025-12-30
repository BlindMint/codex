/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import us.blindmint.codex.data.local.data_store.DataStore
import us.blindmint.codex.data.local.data_store.DataStoreImpl
import us.blindmint.codex.data.mapper.book.BookMapper
import us.blindmint.codex.data.mapper.book.BookMapperImpl
import us.blindmint.codex.data.mapper.color_preset.ColorPresetMapper
import us.blindmint.codex.data.mapper.color_preset.ColorPresetMapperImpl
import us.blindmint.codex.data.mapper.history.HistoryMapper
import us.blindmint.codex.data.mapper.history.HistoryMapperImpl
import us.blindmint.codex.data.parser.FileParser
import us.blindmint.codex.data.parser.FileParserImpl
import us.blindmint.codex.data.parser.TextParser
import us.blindmint.codex.data.parser.TextParserImpl
import us.blindmint.codex.data.repository.BookRepositoryImpl
import us.blindmint.codex.data.repository.ColorPresetRepositoryImpl
import us.blindmint.codex.data.repository.DataStoreRepositoryImpl
import us.blindmint.codex.data.repository.FileSystemRepositoryImpl
import us.blindmint.codex.data.repository.HistoryRepositoryImpl
import us.blindmint.codex.data.repository.PermissionRepositoryImpl
import us.blindmint.codex.domain.repository.BookRepository
import us.blindmint.codex.domain.repository.ColorPresetRepository
import us.blindmint.codex.domain.repository.DataStoreRepository
import us.blindmint.codex.domain.repository.FileSystemRepository
import us.blindmint.codex.domain.repository.HistoryRepository
import us.blindmint.codex.domain.repository.PermissionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindDataStore(
        dataStoreImpl: DataStoreImpl
    ): DataStore

    @Binds
    @Singleton
    abstract fun bindBookRepository(
        bookRepositoryImpl: BookRepositoryImpl
    ): BookRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(
        historyRepositoryImpl: HistoryRepositoryImpl
    ): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindColorPresetRepository(
        colorPresetRepositoryImpl: ColorPresetRepositoryImpl
    ): ColorPresetRepository

    @Binds
    @Singleton
    abstract fun bindDataStoreRepository(
        dataStoreRepositoryImpl: DataStoreRepositoryImpl
    ): DataStoreRepository

    @Binds
    @Singleton
    abstract fun bindFileSystemRepository(
        fileSystemRepositoryImpl: FileSystemRepositoryImpl
    ): FileSystemRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(
        permissionRepositoryImpl: PermissionRepositoryImpl
    ): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindBookMapper(
        bookMapperImpl: BookMapperImpl
    ): BookMapper

    @Binds
    @Singleton
    abstract fun bindHistoryMapper(
        historyMapperImpl: HistoryMapperImpl
    ): HistoryMapper

    @Binds
    @Singleton
    abstract fun bindColorPresetMapper(
        colorPresetMapperImpl: ColorPresetMapperImpl
    ): ColorPresetMapper

    @Binds
    @Singleton
    abstract fun bindFileParser(
        fileParserImpl: FileParserImpl
    ): FileParser

    @Binds
    @Singleton
    abstract fun bindTextParser(
        textParserImpl: TextParserImpl
    ): TextParser
}