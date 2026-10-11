package com.echo.inbox.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.echo.inbox.core.util.ContentIdentityHasher
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * HMACs message identity material with a non-exportable Android Keystore key.
 *
 * Echo Inbox stores the resulting digest only inside the encrypted database. This lets the diff
 * engine distinguish two source messages that have the same redacted display text (for example,
 * two different OTPs) without retaining the original sensitive value.
 */
class AndroidKeystoreIdentityHasher : ContentIdentityHasher {
    private val key: SecretKey by lazy(LazyThreadSafetyMode.SYNCHRONIZED, ::loadOrCreateKey)
    override fun digest(value: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(key)
        return mac.doFinal(value.toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            KEYSTORE_PROVIDER,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "echo_inbox_message_identity_hmac_v1"
        const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
