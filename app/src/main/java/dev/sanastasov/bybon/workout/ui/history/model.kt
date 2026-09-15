package dev.sanastasov.bybon.workout.ui.history

import android.net.Uri
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState
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
    val id: WorkoutSessionId,
    val planName: String,
    val date: LocalDate,
    val exercises: List<ExerciseTopSetUi>,
)

data class ExerciseTopSetUi(
    val name: String,
    val weightKg: String,
    val reps: Int,
    val oneRm: Weight,
)

sealed class WorkoutHistoryAction {
    data class OnCsvSelected(
        val uri: Uri,
    ) : WorkoutHistoryAction()
    data object OnImportDone : WorkoutHistoryAction()
}

internal fun List<WorkoutSession>.toHistoryUi(): List<WorkoutSessionHistoryUi> =
    filter { it.state is WorkoutState.Completed }
        .sortedByDescending { it.startedAt }
        .map { it.toHistoryUi() }

internal fun WorkoutSession.toHistoryUi(): WorkoutSessionHistoryUi {
    check(state is WorkoutState.Completed) {
        "History UI requires a completed session, was $state"
    }
    return WorkoutSessionHistoryUi(
        id = id,
        planName = planName,
        date = startedAt.toLocalDate(),
        exercises = exercises.mapNotNull { it.toTopSetUi() },
    )
}

private fun WorkoutExercise.toTopSetUi(): ExerciseTopSetUi? {
    val topSet = sets.maxWithOrNull(
        compareBy(
            { it.weight },
            { it.reps },
        ),
    ) ?: return null
    return ExerciseTopSetUi(
        name = exerciseDefinition.name,
        weightKg = topSet.weight.kilograms,
        reps = topSet.reps,
        oneRm = topSet.oneRm,
    )
}
