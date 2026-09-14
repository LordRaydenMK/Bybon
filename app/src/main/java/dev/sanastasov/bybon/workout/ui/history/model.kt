package dev.sanastasov.bybon.workout.ui.history

import android.net.Uri
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.completedSessionKey
import java.time.LocalDate

sealed class WorkoutHistoryUiState {
    data object Loading : WorkoutHistoryUiState()
    data object Empty : WorkoutHistoryUiState()
    data object Importing : WorkoutHistoryUiState()
    data class Summary(
        val summary: ImportSummaryUi,
    ) : WorkoutHistoryUiState()
    data class History(
        val sessions: List<WorkoutSessionHistoryUi>,
    ) : WorkoutHistoryUiState()
}

data class ImportSummaryUi(
    val sessionCount: Int,
    val sessionsByPlan: List<PlanSessionCountUi>,
    val plansCreatedCount: Int,
    val exercisesImportedCount: Int,
    val firstSessionDate: LocalDate?,
    val lastSessionDate: LocalDate?,
    val workingSetCount: Int,
)

data class PlanSessionCountUi(
    val planName: String,
    val sessionCount: Int,
)

data class WorkoutSessionHistoryUi(
    val key: String,
    val planName: String,
    val date: LocalDate,
    val exercises: List<ExerciseTopSetUi>,
)

data class ExerciseTopSetUi(
    val name: String,
    val weightKg: String,
    val reps: Int,
    val estimatedOneRmKg: Float?,
)

sealed class WorkoutHistoryAction {
    data class OnCsvSelected(
        val uri: Uri,
    ) : WorkoutHistoryAction()
    data object OnImportDone : WorkoutHistoryAction()
}

internal fun List<WorkoutSession>.toHistoryUi(): List<WorkoutSessionHistoryUi> =
    filter { it.state is WorkoutState.Completed }
        .sortedByDescending { (it.state as WorkoutState.Completed).startedAt }
        .map { it.toHistoryUi() }

internal fun WorkoutSession.toHistoryUi(): WorkoutSessionHistoryUi {
    val completed = state as WorkoutState.Completed
    return WorkoutSessionHistoryUi(
        key = checkNotNull(completedSessionKey()),
        planName = planName,
        date = completed.startedAt.toLocalDate(),
        exercises = exercises.mapNotNull { it.toTopSetUi() },
    )
}

private fun WorkoutExercise.toTopSetUi(): ExerciseTopSetUi? {
    val topSet = sets
        .filter { it.setState == SetState.Completed }
        .maxWithOrNull(
            compareBy(
                { it.weight.kilogramsValue },
                { it.reps },
            ),
        ) ?: return null
    return ExerciseTopSetUi(
        name = exerciseDefinition.name,
        weightKg = topSet.weight.kilograms,
        reps = topSet.reps,
        estimatedOneRmKg = topSet.oneRm,
    )
}
