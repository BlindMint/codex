/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.local.dto

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

enum class OpdsSourceStatus {
    UNKNOWN,
    CONNECTING,
    CONNECTED,
    AUTH_FAILED,
    CONNECTION_FAILED,
    DISABLED
}

@Entity
@Parcelize
data class OpdsSourceEntity(
    @PrimaryKey(true) val id: Int = 0,
    val name: String,
    val url: String,
    @Deprecated("Use usernameEncrypted instead - credentials are encrypted at rest") val username: String? = null,
    @Deprecated("Use passwordEncrypted instead - credentials are encrypted at rest") val password: String? = null,
    val usernameEncrypted: String? = null,
    val passwordEncrypted: String? = null,
    val enabled: Boolean = true,
    val lastSync: Long = 0,
    val status: OpdsSourceStatus = OpdsSourceStatus.UNKNOWN
) : Parcelable {
    val hasCredentials: Boolean
        get() = !usernameEncrypted.isNullOrBlank()
}