/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.remote.dto.opds2

import kotlinx.serialization.Serializable

/**
 * Link object in OPDS 2.0 format.
 * Used for acquisition links, navigation, and related resources.
 */
@Serializable
data class Opds2LinkDto(
    val href: String = "",
    val type: String? = null,
    val rel: String? = null,
    val title: String? = null,
    val templated: Boolean? = null,
    val height: Int? = null,
    val width: Int? = null,
    val bitrate: Double? = null,
    val duration: Double? = null,
    val language: List<String> = emptyList(),
    val alternate: List<Opds2LinkDto> = emptyList(),
    val children: List<Opds2LinkDto> = emptyList(),
    val properties: Opds2LinkPropertiesDto? = null
)

/**
 * Link properties for additional metadata about the linked resource.
 */
@Serializable
data class Opds2LinkPropertiesDto(
    val numberOfItems: Int? = null,
    val price: Opds2PriceDto? = null,
    val indirectAcquisition: List<Opds2IndirectAcquisitionDto> = emptyList(),
    val holds: Opds2HoldsDto? = null,
    val copies: Opds2CopiesDto? = null,
    val availability: Opds2AvailabilityDto? = null
)

/**
 * Price information for paid acquisitions.
 */
@Serializable
data class Opds2PriceDto(
    val currency: String = "",
    val value: Double = 0.0
)

/**
 * Indirect acquisition for DRM-protected content.
 */
@Serializable
data class Opds2IndirectAcquisitionDto(
    val type: String = "",
    val child: List<Opds2IndirectAcquisitionDto> = emptyList()
)

/**
 * Library holds information.
 */
@Serializable
data class Opds2HoldsDto(
    val total: Int? = null,
    val position: Int? = null
)

/**
 * Library copies information.
 */
@Serializable
data class Opds2CopiesDto(
    val total: Int? = null,
    val available: Int? = null
)

/**
 * Availability status for library content.
 */
@Serializable
data class Opds2AvailabilityDto(
    val state: String? = null, // available, unavailable, reserved, ready
    val since: String? = null,
    val until: String? = null
)

/**
 * Image resource in OPDS 2.0.
 */
@Serializable
data class Opds2ImageDto(
    val href: String = "",
    val type: String? = null,
    val height: Int? = null,
    val width: Int? = null
)
