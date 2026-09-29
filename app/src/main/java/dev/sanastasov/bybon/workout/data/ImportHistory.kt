package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ImportHistoryResult
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal fun applyHistoryImport(
    exercises: MutableStateFlow<List<ExerciseDefinition>>,
    plans: MutableStateFlow<List<WorkoutPlan>>,
    sessions: MutableStateFlow<List<WorkoutSession>>,
    incomingPlans: List<WorkoutPlan>,
    incomingSessions: List<WorkoutSession>,
    incomingExercises: List<ExerciseDefinition>,
): ImportHistoryResult {
    val addedExercises = exercises.appendDistinct(incomingExercises) { it.id }
    val addedPlans = plans.appendDistinct(incomingPlans) { it.id }
    val addedSessions = sessions.appendDistinct(incomingSessions) { it.id }
    return ImportHistoryResult(
        plans = addedPlans.added,
        sessions = addedSessions.added,
        exercises = addedExercises.added,
        sessionsSkipped = addedSessions.skipped,
    )
}

private data class AppendResult<T>(
    val added: List<T>,
    val skipped: Int,
)

private fun <T, I> MutableStateFlow<List<T>>.appendDistinct(
    incoming: List<T>,
    id: (T) -> I,
): AppendResult<T> {
    var result = AppendResult(emptyList<T>(), incoming.size)
    update { current ->
        val known = current.map(id).toMutableSet()
        val added = mutableListOf<T>()
        for (item in incoming) {
            val itemId = id(item)
            if (itemId in known) continue
            added += item
            known += itemId
        }
        result = AppendResult(added, incoming.size - added.size)
        current + added
    }
    return result
}
