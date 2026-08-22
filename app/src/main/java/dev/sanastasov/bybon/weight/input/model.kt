package dev.sanastasov.bybon.weight.input

import dev.sanastasov.bybon.weight.BodyWeight
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class WeightInputUi(
    val date: LocalDate,
    val savedBodyWeight: BodyWeight? = null,
) {

    val dayOfWeek: String
        get() = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)

    val previousDate: LocalDate = date.minusDays(1)

    val nextDate: LocalDate? = date.plusDays(1).takeIf { it <= LocalDate.now() }
}

sealed class WeightInputAction {
    data class OnWeightChanged(val weight: String) : WeightInputAction()
    data class OnSaveWeight(val date: LocalDate, val weight: String) : WeightInputAction()
    data class OnNewDateSelected(val date: LocalDate) : WeightInputAction()

    data class AddWeight(val amount: BodyWeight) : WeightInputAction()
    data class RemoveWeight(val amount: BodyWeight) : WeightInputAction()
}