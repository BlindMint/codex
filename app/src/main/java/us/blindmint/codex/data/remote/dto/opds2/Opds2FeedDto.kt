/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.remote.dto.opds2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OPDS 2.0 Feed DTO for JSON-based feeds.
 * See: https://drafts.opds.io/opds-2.0
 */
@Serializable
data class Opds2FeedDto(
    val metadata: Opds2FeedMetadataDto? = null,
    val links: List<Opds2LinkDto> = emptyList(),
    val navigation: List<Opds2NavigationDto> = emptyList(),
    val publications: List<Opds2PublicationDto> = emptyList(),
    val groups: List<Opds2GroupDto> = emptyList(),
    val facets: List<Opds2FacetDto> = emptyList()
)

/**
 * Feed-level metadata for OPDS 2.0
 */
@Serializable
data class Opds2FeedMetadataDto(
    val title: String = "",
    val subtitle: String? = null,
    val modified: String? = null,
    @SerialName("@type")
    val type: String? = null,
    val numberOfItems: Int? = null,
    val itemsPerPage: Int? = null,
    val currentPage: Int? = null
)

/**
 * Navigation entry for catalog navigation
 */
@Serializable
data class Opds2NavigationDto(
    val href: String = "",
    val title: String = "",
    val rel: String? = null,
    val type: String? = null
)

/**
 * Group for organizing publications
 */
@Serializable
data class Opds2GroupDto(
    val metadata: Opds2FeedMetadataDto? = null,
    val links: List<Opds2LinkDto> = emptyList(),
    val navigation: List<Opds2NavigationDto> = emptyList(),
    val publications: List<Opds2PublicationDto> = emptyList()
)

/**
 * Facet for filtering/sorting options
 */
@Serializable
data class Opds2FacetDto(
    val metadata: Opds2FacetMetadataDto? = null,
    val links: List<Opds2LinkDto> = emptyList()
)

@Serializable
data class Opds2FacetMetadataDto(
    val title: String = "",
    val numberOfItems: Int? = null
)
