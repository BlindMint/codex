/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.remote.dto

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "entry", strict = false)
data class OpdsEntryDto(
    @field:Element(name = "id")
    var id: String = "",

    @field:Element(name = "title")
    var title: String = "",

    @field:Element(name = "author", required = false)
    var author: String? = null,

    @field:Element(name = "summary", required = false)
    var summary: String? = null,

    @field:Element(name = "published", required = false)
    var published: String? = null,

    @field:Element(name = "language", required = false)
    var language: String? = null,

    @field:Element(name = "publisher", required = false)
    var publisher: String? = null,

    @field:Element(name = "rights", required = false)
    var rights: String? = null,

    @field:ElementList(name = "identifier", inline = true, required = false)
    var identifiers: List<String> = emptyList(),

    @field:ElementList(name = "category", inline = true, required = false)
    var categories: List<OpdsCategoryDto> = emptyList(),

    @field:ElementList(name = "link", inline = true, required = false)
    var links: List<OpdsLinkDto> = emptyList()
)