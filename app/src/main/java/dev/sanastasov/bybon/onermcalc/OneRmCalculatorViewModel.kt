package dev.sanastasov.bybon.onermcalc

import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class OneRmCalculatorViewModel {

    val weight = mutableStateOf("50")

    val reps = mutableStateOf("10")

    val uiState: StateFlow<OneRmUiState>
        field = MutableStateFlow(
            OneRmUiState(
                OneRmEntry(
                    weight.value.toFloat(),
                    reps.value.toInt()
                )
            )
        )

    fun onAction(action: OneRmCalcAction) {
        when (action) {
            is OneRmCalcAction.OnRepsChanged -> onRepsChanged(action.newReps)
            is OneRmCalcAction.OnUpdateReps -> onUpdateReps(action.amount)
            is OneRmCalcAction.OnUpdateWeight -> onUpdateWeight(action.amount)
            is OneRmCalcAction.OnWeightChanged -> onWeightChanged(action.newWeight)
        }
    }

    private fun onWeightChanged(newWeight: String) {
        weight.value = newWeight
    }

    private fun onRepsChanged(newReps: String) {
        reps.value = newReps
    }

    private fun onUpdateWeight(weightToAdd: Float) {
        val weight = (weight.value.toFloat() + weightToAdd).toString()
        onWeightChanged(weight)
        reps.value.toIntOrNull()?.let { reps ->
            recalculateHistory(weight.toFloat(), reps, uiState.value.history).also { history ->
                uiState.update { it.copy(history = history) }
            }
        }
    }

    private fun onUpdateReps(repsToAdd: Int) {
        val reps = (reps.value.toInt() + repsToAdd).toString()
        onRepsChanged(reps)
        weight.value.toFloatOrNull()?.let { weight ->
            recalculateHistory(weight, reps.toInt(), uiState.value.history).also { history ->
                uiState.update { it.copy(history = history) }
            }
        }
    }

    private fun recalculateHistory(
        weight: Float,
        reps: Int,
        history: List<OneRmEntry>
    ): List<OneRmEntry> {
        val newEntry = OneRmEntry(weight, reps)
        return buildList {
            add(newEntry)
            addAll(history.take(10))
            sortByDescending { it.calculate1Rm() }
        }.distinct()
    }
}