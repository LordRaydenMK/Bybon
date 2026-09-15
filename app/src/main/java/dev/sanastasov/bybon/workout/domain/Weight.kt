package dev.sanastasov.bybon.workout.domain

import java.util.Locale
import kotlin.math.roundToInt

@JvmInline
value class Weight(
    private val value: Int,
) : Comparable<Weight> {

    init {
        require(value > 0) { "Weight must be positive" }
    }

    val kilograms: String
        get() = "%.2f".format(Locale.US, kilogramsValue).trimEnd('0').trimEnd('.')

    val kilogramsValue: Float
        get() = value / 100f

    operator fun plus(other: Weight): Weight = Weight(value + other.value)

    operator fun minus(other: Weight): Weight =
        minusOrNull(other) ?: throw IllegalArgumentException("Weight must be positive")

    fun minusOrNull(other: Weight): Weight? {
        val result = value - other.value
        return if (result > 0) Weight(result) else null
    }

    override fun compareTo(other: Weight): Int = value.compareTo(other.value)

    companion object {

        fun kilograms(value: Int): Weight = Weight(value * 100)

        fun kilograms(value: Float): Weight =
            kilogramsOrNull(value) ?: throw IllegalArgumentException("Weight must be positive")

        fun kilogramsOrNull(value: Float): Weight? {
            val hundredths = (value * 100).roundToInt()
            return if (hundredths > 0) Weight(hundredths) else null
        }

        fun parseString(value: String): Weight = kilograms(value.toFloat())
    }
}
