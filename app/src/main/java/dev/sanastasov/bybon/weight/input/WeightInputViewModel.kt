package dev.sanastasov.bybon.weight.input

import androidx.compose.runtime.mutableStateOf
import dev.sanastasov.bybon.weight.BodyWeight
import dev.sanastasov.bybon.weight.BodyWeightEntry
import dev.sanastasov.bybon.weight.WeightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeightInputViewModel(
    val repository: WeightRepository,
    val coroutineScope: CoroutineScope,
) {

    val weight = mutableStateOf("")

    val uiState: StateFlow<WeightInputUi>
        field = MutableStateFlow(WeightInputUi(LocalDate.now()))

    init {
        coroutineScope.launch {
            prefillMostRecentWeight()
        }
    }

    fun onAction(action: WeightInputAction) {
        when (action) {
            is WeightInputAction.OnSaveWeight -> saveWeight(action)
            is WeightInputAction.OnWeightChanged -> weight.value = action.weight
            is WeightInputAction.OnNewDateSelected -> updateDate(action)
            is WeightInputAction.OnUpdateWeight -> weight.value =
                (BodyWeight.parseFromString(weight.value) + action.amount).kilograms.toString()
        }
    }

    private suspend fun prefillMostRecentWeight() {
        val mostRecentEntry = repository.entries().firstOrNull()?.lastOrNull()
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

    private fun updateDate(action: WeightInputAction.OnNewDateSelected) {
        uiState.update { it.copy(date = action.date) }
    }
}