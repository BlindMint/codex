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

    // Changed from @Element to @ElementList to support multiple authors per OPDS/Atom spec
    @field:ElementList(entry = "author", inline = true, required = false)
    var authors: MutableList<OpdsAuthorDto> = mutableListOf(),

    @field:Element(name = "summary", required = false)
    var summary: String? = null,

    // Support both 'content' and 'summary' elements for description
    @field:Element(name = "content", required = false)
    var content: String? = null,

    @field:Element(name = "published", required = false)
    var published: String? = null,

    @field:Element(name = "updated", required = false)
    var updated: String? = null,

    @field:Element(name = "language", required = false)
    var language: String? = null,

    @field:Element(name = "publisher", required = false)
    var publisher: String? = null,

    @field:Element(name = "rights", required = false)
    var rights: String? = null,

    @field:ElementList(entry = "identifier", inline = true, required = false)
    var identifiers: MutableList<String> = mutableListOf(),

    @field:ElementList(entry = "category", inline = true, required = false)
    var categories: MutableList<OpdsCategoryDto> = mutableListOf(),

    @field:ElementList(entry = "link", inline = true, required = false)
    var links: MutableList<OpdsLinkDto> = mutableListOf()
)