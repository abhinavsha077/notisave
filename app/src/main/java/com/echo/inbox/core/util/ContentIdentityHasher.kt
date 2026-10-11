package com.echo.inbox.core.util

/** Produces an opaque stable identity for sensitive source content. */
fun interface ContentIdentityHasher {
    fun digest(value: String): String

    companion object {
        /** Deterministic fallback used by dependency-free JVM tests. */
        val SHA256 = ContentIdentityHasher(Hashing::sha256)
    }
}
