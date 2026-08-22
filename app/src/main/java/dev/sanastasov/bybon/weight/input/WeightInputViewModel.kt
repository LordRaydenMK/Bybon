package dev.sanastasov.bybon.weight.input

import androidx.compose.runtime.mutableStateOf
import dev.sanastasov.bybon.weight.WeightEntry
import dev.sanastasov.bybon.weight.WeightRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class WeightInputViewModel(
    val repository: WeightRepository,
    val coroutineScope: CoroutineScope,
) {

    val weight = mutableStateOf("")

    fun onAction(action: WeightInputAction) {
        when (action) {
            is WeightInputAction.OnSaveWeight -> saveWeight(action)
            is WeightInputAction.OnWeightChanged -> weight.value = action.weight
        }
    }

    private fun saveWeight(weight: WeightInputAction.OnSaveWeight) {
        coroutineScope.launch {
            repository.insert(WeightEntry(LocalDate.now(), weight.weight.toFloat()))
        }
    }
}