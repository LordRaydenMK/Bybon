package dev.sanastasov.bybon.weight.input

import androidx.compose.runtime.mutableStateOf
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.weight.BodyWeight
import dev.sanastasov.bybon.weight.BodyWeightEntry
import dev.sanastasov.bybon.weight.WeightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeightInputViewModel(
    val repository: WeightRepository,
    val coroutineScope: CoroutineScope,
) {

    val weight = mutableStateOf("")

    private val date = MutableStateFlow<LocalDate>(LocalDate.now())

    val uiState: StateFlow<WeightInputUi> = combine(
        repository.entries(),
        date,
    ) { allEntries, date ->
        WeightInputUi(date, allEntries.firstOrNull { it.date == date }?.weight)
    }
        .onEach {
            if (it.savedBodyWeight != null) weight.value = it.savedBodyWeight.kilograms.toString()
        }
        .stateInWhileInForeground(
            coroutineScope,
            WeightInputUi(date.value)
        )

    init {
        coroutineScope.launch {
            prefillMostRecentWeight()
        }
    }

    fun onAction(action: WeightInputAction) {
        when (action) {
            is WeightInputAction.OnSaveWeight -> saveWeight(action)
            is WeightInputAction.OnWeightChanged -> weight.value = action.weight
            is WeightInputAction.OnNewDateSelected -> date.value = action.date
            is WeightInputAction.OnUpdateWeight -> weight.value =
                (BodyWeight.parseFromString(weight.value) + action.amount).kilograms.toString()
        }
    }

    private suspend fun prefillMostRecentWeight() {
        val mostRecentEntry = repository.entries().firstOrNull()?.firstOrNull()
        if (mostRecentEntry != null && weight.value.isBlank()) {
            weight.value = mostRecentEntry.weight.kilograms.toString()
        }
    }

    private fun saveWeight(weight: WeightInputAction.OnSaveWeight) {
        coroutineScope.launch {
            repository.insert(
                BodyWeightEntry(
                    weight.date,
                    BodyWeight.parseFromString(weight.weight)
                )
            )
        }
    }
}