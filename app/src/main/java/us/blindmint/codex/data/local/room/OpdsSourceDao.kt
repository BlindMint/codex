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
import us.blindmint.codex.data.local.dto.OpdsSourceEntity

@Dao
interface OpdsSourceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpdsSource(opdsSource: OpdsSourceEntity): Long

    @Update
    suspend fun updateOpdsSource(opdsSource: OpdsSourceEntity)

    @Upsert
    suspend fun upsertOpdsSource(opdsSource: OpdsSourceEntity)

    @Delete
    suspend fun deleteOpdsSource(opdsSource: OpdsSourceEntity)

    @Query("SELECT * FROM OpdsSourceEntity")
    suspend fun getAllOpdsSources(): List<OpdsSourceEntity>

    @Query("SELECT * FROM OpdsSourceEntity WHERE id = :id")
    suspend fun getOpdsSourceById(id: Int): OpdsSourceEntity?

    @Query("DELETE FROM OpdsSourceEntity WHERE id = :id")
    suspend fun deleteOpdsSourceById(id: Int)
}