package br.f21campo.domain

import java.security.MessageDigest

object Sha256 {
    fun of(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    fun isValid(value: String): Boolean = value.matches(Regex("[0-9a-f]{64}"))
}
