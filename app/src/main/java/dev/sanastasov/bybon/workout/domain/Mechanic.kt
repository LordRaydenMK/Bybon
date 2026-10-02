package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class Mechanic {
    Compound,
    Isolation,
}

val Mechanic.defaultRest: Duration
    get() = when (this) {
        Mechanic.Compound -> 2.minutes
        Mechanic.Isolation -> 1.minutes
    }
