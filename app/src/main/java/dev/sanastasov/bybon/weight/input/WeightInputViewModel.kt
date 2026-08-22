package dev.sanastasov.bybon.weight.input

import androidx.compose.runtime.mutableStateOf
import dev.sanastasov.bybon.weight.WeightEntry
import dev.sanastasov.bybon.weight.WeightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun onAction(action: WeightInputAction) {
        when (action) {
            is WeightInputAction.OnSaveWeight -> saveWeight(action)
            is WeightInputAction.OnWeightChanged -> weight.value = action.weight
            is WeightInputAction.OnNewDateSelected -> updateDate(action)
        }
    }

    private fun saveWeight(weight: WeightInputAction.OnSaveWeight) {
        coroutineScope.launch {
            repository.insert(WeightEntry(weight.date, weight.weight.toFloat()))
        }
    }

    private fun updateDate(action: WeightInputAction.OnNewDateSelected) {
        uiState.update { it.copy(date = action.date) }
    }
}