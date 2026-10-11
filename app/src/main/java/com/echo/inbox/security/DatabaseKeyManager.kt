package com.echo.inbox.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DatabaseKeyManager(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getOrCreatePassphrase(): ByteArray {
        val encrypted = preferences.getString(KEY_WRAPPED_PASSPHRASE, null)
        val iv = preferences.getString(KEY_IV, null)
        if (encrypted != null && iv != null) {
            return decrypt(
                Base64.decode(encrypted, Base64.NO_WRAP),
                Base64.decode(iv, Base64.NO_WRAP),
            )
        }

        val passphrase = ByteArray(32).also(SecureRandom()::nextBytes)
        val wrapped = encrypt(passphrase)
        val stored = preferences.edit()
            .putString(KEY_WRAPPED_PASSPHRASE, Base64.encodeToString(wrapped.ciphertext, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(wrapped.iv, Base64.NO_WRAP))
            .commit()
        if (!stored) {
            passphrase.fill(0)
            throw DatabaseKeyUnavailableException(
                "The encrypted database passphrase could not be persisted.",
            )
        }
        return passphrase
    }

    private fun encrypt(plaintext: ByteArray): WrappedValue {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        return WrappedValue(cipher.doFinal(plaintext), cipher.iv)
    }

    private fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateWrappingKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(ciphertext)
        } catch (error: Exception) {
            throw DatabaseKeyUnavailableException(
                "The Android Keystore key can no longer decrypt the notification database passphrase.",
                error,
            )
        }
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private data class WrappedValue(val ciphertext: ByteArray, val iv: ByteArray)

    companion object {
        private const val PREFERENCES_NAME = "database_key_material"
        private const val KEY_WRAPPED_PASSPHRASE = "wrapped_passphrase"
        private const val KEY_IV = "wrapped_passphrase_iv"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "echo_inbox_database_wrapping_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

class DatabaseKeyUnavailableException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
