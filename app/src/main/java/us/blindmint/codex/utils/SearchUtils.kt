/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.utils

/**
 * Native fuzzy search utilities using Levenshtein distance.
 * Replaces FuzzyWuzzy for better performance and no external dependencies.
 */

/**
 * Computes Levenshtein distance (edit distance) between two strings.
 * Optimized with early termination if distance exceeds maxThreshold.
 */
fun levenshteinDistance(s1: String, s2: String, maxThreshold: Int = Int.MAX_VALUE): Int {
    val len1 = s1.length
    val len2 = s2.length
    if (len1 == 0) return len2
    if (len2 == 0) return len1

    var dp = IntArray(len1 + 1) { it }  // Previous row
    var current = IntArray(len1 + 1)

    for (j in 1..len2) {
        current[0] = j
        var minInRow = j  // Track min in current row for early exit
        for (i in 1..len1) {
            val cost = if (s1[i-1] == s2[j-1]) 0 else 1
            current[i] = minOf(
                dp[i] + 1,        // deletion
                current[i-1] + 1, // insertion
                dp[i-1] + cost    // substitution
            )
            if (current[i] < minInRow) minInRow = current[i]
        }
        if (minInRow > maxThreshold) return maxThreshold + 1  // Early exit
        val temp = dp
        dp = current
        current = temp
    }
    return dp[len1]
}

/**
 * Computes minimum Levenshtein distance to any substring of s2 that is within length range [minLen, maxLen].
 * Used for partial fuzzy matching (e.g., "altrd" vs substrings of "Altered States...").
 */
fun minSubstringDistance(query: String, target: String, threshold: Int): Int {
    val qLen = query.length
    if (qLen == 0) return 0
    val tLen = target.length
    if (tLen < qLen) return levenshteinDistance(query, target, threshold)

    var minDist = Int.MAX_VALUE
    val maxLen = qLen + 2  // Allow slightly longer substrings for insertions
    for (start in 0..(tLen - qLen)) {
        val end = minOf(start + maxLen, tLen)
        for (len in qLen..end) {
            if (start + len > tLen) break
            val substr = target.substring(start, start + len)
            val dist = levenshteinDistance(query, substr, threshold)
            if (dist < minDist) minDist = dist
            if (minDist == 0) return 0  // Perfect match
        }
    }
    return if (minDist > threshold) threshold + 1 else minDist
}

/**
 * Computes a similarity score (0-100) based on min substring distance.
 * Higher score means better match. Equivalent to FuzzyWuzzy's partialRatio.
 */
fun partialSimilarity(query: String, target: String): Int {
    val qLen = query.length
    if (qLen == 0) return 100
    val dist = minSubstringDistance(query, target, qLen)  // Threshold = query length for normalization
    return ((1.0 - dist.toDouble() / qLen) * 100).toInt().coerceIn(0, 100)
}