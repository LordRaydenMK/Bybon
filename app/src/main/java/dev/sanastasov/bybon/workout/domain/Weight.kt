package dev.sanastasov.bybon.workout.domain

import java.util.Locale
import kotlin.math.roundToInt

@JvmInline
value class Weight(
    private val value: Int,
) : Comparable<Weight> {

    val kilograms: String
        get() = "%.2f".format(Locale.US, kilogramsValue).trimEnd('0').trimEnd('.')

    val kilogramsValue: Float
        get() = value / 100f

    operator fun plus(other: Weight): Weight = Weight(value + other.value)

    operator fun minus(other: Weight): Weight = Weight((value - other.value).coerceAtLeast(0))

    override fun compareTo(other: Weight): Int = value.compareTo(other.value)

    companion object {

        fun kilograms(value: Int): Weight = Weight(value * 100)

        fun kilograms(value: Float): Weight = Weight((value * 100).roundToInt())

        fun parseString(value: String): Weight = kilograms(value.toFloat())
    }
}
