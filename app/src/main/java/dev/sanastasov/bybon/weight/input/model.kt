package dev.sanastasov.bybon.weight.input

import java.time.LocalDate

data class WeightInputUi(val date: LocalDate) {

    val previousDate: LocalDate = date.minusDays(1)

    val nextDate: LocalDate? = date.plusDays(1).takeIf { it <= LocalDate.now() }
}

sealed class WeightInputAction {
    data class OnWeightChanged(val weight: String) : WeightInputAction()
    data class OnSaveWeight(val date: LocalDate, val weight: String) : WeightInputAction()

    data class OnNewDateSelected(val date: LocalDate) : WeightInputAction()
}