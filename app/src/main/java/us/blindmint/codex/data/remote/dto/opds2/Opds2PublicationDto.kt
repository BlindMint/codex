/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.remote.dto.opds2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OPDS 2.0 Publication DTO representing a book/document.
 * Contains metadata, acquisition links, and images.
 */
@Serializable
data class Opds2PublicationDto(
    val metadata: Opds2MetadataDto = Opds2MetadataDto(),
    val links: List<Opds2LinkDto> = emptyList(),
    val images: List<Opds2ImageDto> = emptyList()
)

/**
 * Publication metadata following the Readium Web Publication Manifest format.
 */
@Serializable
data class Opds2MetadataDto(
    @SerialName("@type")
    val type: String? = null,
    val identifier: String? = null,
    val title: String = "",
    val subtitle: String? = null,
    val sortAs: String? = null,
    val author: List<Opds2ContributorDto> = emptyList(),
    val translator: List<Opds2ContributorDto> = emptyList(),
    val editor: List<Opds2ContributorDto> = emptyList(),
    val contributor: List<Opds2ContributorDto> = emptyList(),
    val publisher: List<Opds2ContributorDto> = emptyList(),
    val language: List<String> = emptyList(),
    val modified: String? = null,
    val published: String? = null,
    val description: String? = null,
    val subject: List<Opds2SubjectDto> = emptyList(),
    val belongsTo: Opds2BelongsToDto? = null,
    val numberOfPages: Int? = null,
    val duration: Double? = null,
    // Additional common fields
    val rights: String? = null,
    val readingProgression: String? = null
)

/**
 * Contributor (author, publisher, etc.) in OPDS 2.0
 * Can be either a simple string or an object with name/sortAs/links
 */
@Serializable
data class Opds2ContributorDto(
    val name: String = "",
    val sortAs: String? = null,
    val identifier: String? = null,
    val links: List<Opds2LinkDto> = emptyList()
)

/**
 * Subject/Category in OPDS 2.0
 */
@Serializable
data class Opds2SubjectDto(
    val name: String = "",
    val sortAs: String? = null,
    val code: String? = null,
    val scheme: String? = null,
    val links: List<Opds2LinkDto> = emptyList()
)

/**
 * Collection/Series information
 */
@Serializable
data class Opds2BelongsToDto(
    val series: List<Opds2SeriesDto> = emptyList(),
    val collection: List<Opds2CollectionDto> = emptyList()
)

@Serializable
data class Opds2SeriesDto(
    val name: String = "",
    val position: Double? = null,
    val identifier: String? = null,
    val links: List<Opds2LinkDto> = emptyList()
)

@Serializable
data class Opds2CollectionDto(
    val name: String = "",
    val identifier: String? = null,
    val links: List<Opds2LinkDto> = emptyList()
)
