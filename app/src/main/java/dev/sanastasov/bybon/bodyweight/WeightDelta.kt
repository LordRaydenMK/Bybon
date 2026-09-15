package dev.sanastasov.bybon.bodyweight

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Signed weight change, same units as [BodyWeight] (0.01 kg, 0.05 kg grid).
 * Unlike [BodyWeight], zero and negative values are allowed.
 */
@JvmInline
value class WeightDelta(
    val value: Int,
) : Comparable<WeightDelta> {
    init {
        require(value % 5 == 0) {
            "Weight delta must be in 0.05 kg increments. Found '$value'"
        }
    }

    val kilograms: Float
        get() = value / 100f

    fun absolute(): WeightDelta = WeightDelta(abs(value))

    operator fun unaryMinus(): WeightDelta = WeightDelta(-value)

    override fun compareTo(other: WeightDelta): Int = value.compareTo(other.value)

    fun signedKilograms(): String {
        val sign = if (value > 0) "+" else ""
        return "$sign$kilograms"
    }

    fun formatRate(startWeight: BodyWeight): String {
        val percent = if (startWeight.value == 0) {
            0f
        } else {
            value.toFloat() / startWeight.value * 100f
        }
        return "${signedKilograms()} kg/week (${String.format(Locale.US, "%.2f", percent)}%)"
    }

    companion object {
        val Zero = WeightDelta(0)
        val WaterNoise = WeightDelta(50)
    }
}

fun BodyWeight.minusToDelta(other: BodyWeight): WeightDelta = WeightDelta(value - other.value)

fun BodyWeight.percentOf(percent: Float): WeightDelta {
    val raw = value * percent / 100f
    val roundedTo5 = (raw / 5f).roundToInt() * 5
    return WeightDelta(roundedTo5.coerceAtLeast(0))
}

operator fun BodyWeight.plus(delta: WeightDelta): BodyWeight = BodyWeight(value + delta.value)

operator fun BodyWeight.minus(delta: WeightDelta): BodyWeight = BodyWeight(value - delta.value)
