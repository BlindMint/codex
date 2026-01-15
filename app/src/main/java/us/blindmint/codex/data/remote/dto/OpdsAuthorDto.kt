/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.remote.dto

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "author", strict = false)
data class OpdsAuthorDto(
    @field:Element(name = "name", required = false)
    var name: String? = null,

    @field:Element(name = "uri", required = false)
    var uri: String? = null
)