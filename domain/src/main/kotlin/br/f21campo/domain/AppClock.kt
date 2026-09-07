package br.f21campo.domain

import java.time.Instant

fun interface AppClock { fun now(): Instant }

object SystemAppClock : AppClock {
    override fun now(): Instant = Instant.now()
}
