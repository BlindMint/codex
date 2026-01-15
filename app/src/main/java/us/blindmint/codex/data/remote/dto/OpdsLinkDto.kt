/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.remote.dto

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Root

@Root(name = "link", strict = false)
data class OpdsLinkDto(
    @field:Attribute(name = "href")
    var href: String = "",

    @field:Attribute(name = "rel", required = false)
    var rel: String? = null,

    @field:Attribute(name = "type", required = false)
    var type: String? = null,

    @field:Attribute(name = "title", required = false)
    var title: String? = null
)