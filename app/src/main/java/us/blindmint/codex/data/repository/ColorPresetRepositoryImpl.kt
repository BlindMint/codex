/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.repository

import us.blindmint.codex.data.local.room.BookDao
import us.blindmint.codex.data.local.dto.ColorPresetEntity
import us.blindmint.codex.data.mapper.color_preset.ColorPresetMapper
import us.blindmint.codex.domain.reader.ColorPreset
import us.blindmint.codex.domain.repository.ColorPresetRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Color Preset repository.
 * Manages all [ColorPreset] related work.
 */
@Singleton
class ColorPresetRepositoryImpl @Inject constructor(
    database: BookDao,
    private val colorPresetMapper: ColorPresetMapper
) : BaseRepository<ColorPreset, ColorPresetEntity, BookDao>(), ColorPresetRepository {

    override val dao = database

    override suspend fun updateColorPreset(colorPreset: ColorPreset) {
        val existingOrder = dao.getColorPresetOrder(colorPreset.id)
        val order = existingOrder ?: (dao.getColorPresets().maxOfOrNull { it.order } ?: -1) + 1
        dao.updateColorPreset(colorPresetMapper.toColorPresetEntity(colorPreset, order))
    }

    override suspend fun selectColorPreset(colorPreset: ColorPreset) {
        dao.getColorPresets().map {
            it.copy(isSelected = it.id == colorPreset.id)
        }.forEach {
            dao.updateColorPreset(it)
        }
    }

    override suspend fun getColorPresets(): List<ColorPreset> {
        return dao.getColorPresets()
            .sortedBy { it.order }
            .map { colorPresetMapper.toColorPreset(it) }
    }

    override suspend fun reorderColorPresets(orderedColorPresets: List<ColorPreset>) {
        dao.deleteColorPresets()

        orderedColorPresets.forEachIndexed { index, colorPreset ->
            dao.updateColorPreset(
                colorPresetMapper.toColorPresetEntity(colorPreset, order = index)
            )
        }
    }

    override suspend fun deleteColorPreset(colorPreset: ColorPreset) {
        dao.deleteColorPreset(
            colorPresetMapper.toColorPresetEntity(colorPreset, -1)
        )
    }
}
