package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.isAssisted

fun loggedLoadLabel(weight: Weight?, equipment: Equipment?): String = weight?.toLoadLabel(equipment)
    ?: if (equipment?.isAssisted == true) "0" else MISSING_LOAD_LABEL
