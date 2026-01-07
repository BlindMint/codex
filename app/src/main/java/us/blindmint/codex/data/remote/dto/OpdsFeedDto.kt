/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.remote.dto

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "feed", strict = false)
data class OpdsFeedDto(
    @field:Element(name = "title")
    var title: String = "",

    @field:ElementList(name = "entry", inline = true, required = false)
    var entries: MutableList<OpdsEntryDto> = mutableListOf(),

    @field:ElementList(name = "link", inline = true, required = false)
    var links: MutableList<OpdsLinkDto> = mutableListOf()
)