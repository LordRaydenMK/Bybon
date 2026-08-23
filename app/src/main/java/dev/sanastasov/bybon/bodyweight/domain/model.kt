package dev.sanastasov.bybon.bodyweight

import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Body Weight, represented as Int in units of 0.01 kg (0.05 kg accuracy)
 *
 * 65.45 kg represented as 6545
 */
@JvmInline
value class BodyWeight(val value: Int) {
    init {
        require(value > 0) {
            "Weight must be positive. Found '$value'"
        }
        require(value % 5 == 0) {
            "Weight must be in 0.05 kg increments. Found '$value'"
        }
        require(value < 50000) {
            "Weight must be less than 500kg. Found '$value"
        }
    }

    val kilograms: Float
        get() = value / 100f

    operator fun unaryMinus(): BodyWeight = BodyWeight(-value)

    operator fun minus(toRemove: Float): BodyWeight =
        BodyWeight((kilograms - toRemove).roundToInt())

    operator fun plus(other: BodyWeight) = BodyWeight(value + other.value)

    companion object {

        fun parseFromString(value: String): BodyWeight {
            val kilograms = value.toFloatOrNull()
                ?: throw IllegalArgumentException("Invalid body weight: '$value'")

            val rounded = (kilograms / 0.05).roundToInt() * 0.05f

            return BodyWeight((rounded * 100).roundToInt())
        }
    }
}

data class BodyWeightEntry(
    val date: LocalDate,
    val weight: BodyWeight,
)
