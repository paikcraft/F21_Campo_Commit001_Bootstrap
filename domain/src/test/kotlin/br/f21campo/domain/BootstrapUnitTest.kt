package br.f21campo.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class BootstrapUnitTest {
    @Test
    fun domainBootstrapIsReady() {
        assertTrue(DomainBootstrap.READY)
    }
}
