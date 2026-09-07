package br.f21campo.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainPrimitivesTest {
    @Test fun entityIdIsStableAndGenerated() {
        val id = EntityId.new()
        assertTrue(id.value.isNotBlank())
        assertEquals(id, EntityId(id.value))
    }

    @Test fun clockIsInjectable() {
        val expected = Instant.parse("2026-09-07T12:00:00Z")
        assertEquals(expected, AppClock { expected }.now())
    }

    @Test fun sha256IsLowercaseAndValidated() {
        val hash = Sha256.of("abc".toByteArray())
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash)
        assertTrue(Sha256.isValid(hash))
        assertFalse(Sha256.isValid(hash.uppercase()))
    }

    @Test fun provenanceAndResultAreExplicit() {
        assertEquals(ProvenanceSource.OPERATOR, ProvenanceSource.valueOf("OPERATOR"))
        assertTrue(DomainResult.Success(1) is DomainResult.Success)
    }
}
