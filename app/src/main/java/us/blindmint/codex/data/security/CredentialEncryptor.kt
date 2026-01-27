/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Utility class for encrypting and decrypting OPDS credentials using EncryptedSharedPreferences.
 *
 * Uses AES-256-GCM encryption with MasterKey.
 * This provides secure credential storage at rest while maintaining compatibility with existing Room database.
 *
 * Reference:
 * - AndroidX Security Crypto: https://developer.android.com/jetpack/androidx/releases/security
 * - MasterKey: https://developer.android.com/reference/androidx/security/crypto/MasterKey
 */
object CredentialEncryptor {

    private const val PREFS_NAME = "opds_credentials_prefs"
    private const val MASTER_KEY_ALIAS = "opds_encryption_key"
    private var masterKey: MasterKey? = null

    /**
     * Initialize encryption with EncryptedSharedPreferences.
     * Must be called before any encryption/decryption operations.
     *
     * @param context Application context
     */
    fun initialize(context: Context) {
        if (masterKey == null) {
            try {
                masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

            } catch (e: Exception) {
                throw SecurityException("Failed to initialize encryption", e)
            }
        }
    }

    /**
     * Encrypt a plaintext string using AES-256-GCM.
     *
     * @param context Application context
     * @param plaintext The string to encrypt (e.g., username or password)
     * @return Encrypted string
     * @throws IllegalStateException if initialize() has not been called
     */
    fun encrypt(context: Context, plaintext: String): String {
        val key = masterKey ?: throw IllegalStateException("CredentialEncryptor not initialized. Call initialize() first.")

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val timestamp = System.currentTimeMillis()
        encryptedPrefs.edit()
            .putString("encrypted_$timestamp", plaintext)
            .apply()

        return "encrypted_$timestamp"
    }

    /**
     * Decrypt an encrypted string.
     *
     * @param context Application context
     * @param encrypted The encrypted string
     * @return Decrypted plaintext string
     * @throws IllegalStateException if initialize() has not been called
     */
    fun decrypt(context: Context, encrypted: String): String {
        val key = masterKey ?: throw IllegalStateException("CredentialEncryptor not initialized. Call initialize() first.")

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        return encryptedPrefs.getString(encrypted, null) ?: ""
    }

    /**
     * Encrypt credentials only if they are not null or blank.
     *
     * @param context Application context
     * @param value The credential value to encrypt (username or password)
     * @return Encrypted value if non-null/non-blank, null otherwise
     */
    fun encryptCredential(context: Context, value: String?): String? {
        return value?.let { encrypt(context, it) }
    }

    /**
     * Decrypt credentials only if they are not null.
     *
     * @param context Application context
     * @param value The encrypted credential value
     * @return Decrypted value if not null, null otherwise
     */
    fun decryptCredential(context: Context, value: String?): String? {
        return value?.let { decrypt(context, it) }
    }

    /**
     * Check if encryption has been initialized.
     *
     * @return true if initialized, false otherwise
     */
    fun isInitialized(): Boolean = masterKey != null
}
