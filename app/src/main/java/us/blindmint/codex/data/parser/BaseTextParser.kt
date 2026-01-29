/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser

import android.util.Log
import us.blindmint.codex.domain.reader.ReaderText

/**
 * Base class for all TextParsers, providing common error handling pattern.
 *
 * This class abstracts the try-catch error handling pattern that was duplicated
 * across all TextParser implementations. Subclasses only need to implement
 * their parsing logic without worrying about exception handling.
 */
abstract class BaseTextParser : TextParser {

    /**
     * Tag for logging purposes. Subclasses should override this with a
     * meaningful identifier for their parser type (e.g., "EPUB Parser").
     */
    protected abstract val tag: String

    /**
     * Safely executes a parsing block, catching exceptions and logging them.
     *
     * This method wraps parsing logic in a try-catch block, ensuring that
     * any exceptions are caught, logged, and converted to an empty list
     * (which indicates parsing failure in TextParser contract).
     *
     * @param block The parsing logic to execute
     * @return The result of parsing, or an empty list if an exception occurred
     */
    protected inline fun safeParse(block: () -> List<ReaderText>): List<ReaderText> {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(tag, "Exception parsing: ${e.message}", e)
            emptyList()
        }
    }
}
