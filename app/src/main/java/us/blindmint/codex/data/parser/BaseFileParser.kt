/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.parser

import android.util.Log

/**
 * Base class for all FileParsers, providing common error handling pattern.
 *
 * This class abstracts the try-catch error handling pattern that was duplicated
 * across all FileParser implementations. Subclasses only need to implement
 * their parsing logic without worrying about exception handling.
 */
abstract class BaseFileParser : FileParser {

    /**
     * Tag for logging purposes. Subclasses should override this with a
     * meaningful identifier for their parser type (e.g., "EPUB Parser").
     */
    protected open val tag: String = "File Parser"

    /**
     * Safely executes a parsing block, catching exceptions and logging them.
     *
     * This method wraps the parsing logic in a try-catch block, ensuring that
     * any exceptions are caught, logged, and converted to null (which indicates
     * parsing failure in the FileParser contract).
     *
     * @param block The parsing logic to execute
     * @return The result of parsing, or null if an exception occurred
     */
    protected inline fun <T> safeParse(block: () -> T?): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(tag, "Exception parsing: ${e.message}", e)
            null
        }
    }
}
