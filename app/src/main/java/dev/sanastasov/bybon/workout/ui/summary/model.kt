package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.domain.toDisplayDate
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.exercisePerformance
import dev.sanastasov.bybon.workout.domain.latestCompletedExercise
import dev.sanastasov.bybon.workout.domain.withPreviousFrom
import dev.sanastasov.bybon.workout.ui.details.ExerciseOneRmPointUi
import dev.sanastasov.bybon.workout.ui.details.PersonalRecordUi
import dev.sanastasov.bybon.workout.ui.details.toOneRmTrend
import dev.sanastasov.bybon.workout.ui.details.toPersonalRecordUi
import dev.sanastasov.bybon.workout.ui.formatElapsedClock
import dev.sanastasov.bybon.workout.ui.toLoadLabel
import dev.sanastasov.bybon.workout.ui.toStartTimeLabel
import java.time.LocalDateTime

sealed class WorkoutSummaryUiState {
    data object Loading : WorkoutSummaryUiState()
    data class Content(
        val title: String,
        val date: String,
        val startTime: String,
        val duration: String,
        val exercises: List<WorkoutSummaryExerciseUi>,
        val note: String? = null,
    ) : WorkoutSummaryUiState()
}

data class WorkoutSummaryExerciseUi(
    val id: String,
    val name: String,
    val sets: List<WorkoutSummarySetUi>,
    val notes: List<String> = emptyList(),
    val personalRecord: PersonalRecordUi? = null,
    val oneRmTrend: List<ExerciseOneRmPointUi> = emptyList(),
)

data class WorkoutSummarySetUi(
    val number: Int?,
    val weightKg: String,
    val reps: Int,
    val oneRm: Weight?,
    val isWarmup: Boolean = false,
    val previousOneRm: Weight? = null,
)

sealed class WorkoutSummaryAction {
    data class OnOpenExerciseDetails(
        val exerciseId: String,
    ) : WorkoutSummaryAction()
    data object OnDeleteSession : WorkoutSummaryAction()
}

sealed class WorkoutSummaryEffect {
    data class OpenExerciseDetails(
        val exerciseId: String,
    ) : WorkoutSummaryEffect()
    data object NavigateBack : WorkoutSummaryEffect()
}

internal fun List<WorkoutSession>.completedSummary(
    sessionId: WorkoutSessionId,
    now: LocalDateTime,
): WorkoutSummaryUiState.Content? {
    val session = firstOrNull { it.id == sessionId && it.state is WorkoutState.Completed }
        ?: return null
    val completed = filter { it.state is WorkoutState.Completed }
    return session.toSummaryUi(completed, now)
}

internal fun WorkoutSession.toSummaryUi(
    completedSessions: List<WorkoutSession> = listOf(this),
    now: LocalDateTime = startedAt,
): WorkoutSummaryUiState.Content {
    val completed = state
    check(completed is WorkoutState.Completed) {
        "Summary UI requires a completed session, was $completed"
    }
    return WorkoutSummaryUiState.Content(
        title = planName,
        date = startedAt.toLocalDate().toDisplayDate(),
        startTime = startedAt.toStartTimeLabel(),
        duration = completed.duration.formatElapsedClock(),
        exercises = exercises.mapNotNull {
            it.toSummaryExerciseUi(completedSessions, now, startedAt)
        },
        note = note,
    )
}

private fun WorkoutExercise.toSummaryExerciseUi(
    completedSessions: List<WorkoutSession>,
    now: LocalDateTime,
    sessionStartedAt: LocalDateTime,
): WorkoutSummaryExerciseUi? {
    if (sets.isEmpty()) return null
    val previous = completedSessions.latestCompletedExercise(id, before = sessionStartedAt)
    val matched = if (previous != null) withPreviousFrom(previous) else this
    val warmups = matched.warmupSets.orEmpty().map { it.toWarmupSummaryUi() }
    val work = matched.sets.mapIndexed { index, set -> set.toWorkSummaryUi(index + 1) }
    val performance = completedSessions.exercisePerformance(id, now)
    return WorkoutSummaryExerciseUi(
        id = id,
        name = exerciseDefinition.name,
        sets = warmups + work,
        notes = notes,
        personalRecord = performance.personalRecord?.toPersonalRecordUi(),
        oneRmTrend = performance.recentSessions.toOneRmTrend(),
    )
}

private fun ExerciseSet.toWarmupSummaryUi(): WorkoutSummarySetUi = WorkoutSummarySetUi(
    number = null,
    weightKg = weight.toLoadLabel(),
    reps = reps,
    oneRm = null,
    isWarmup = true,
)

private fun ExerciseSet.toWorkSummaryUi(number: Int): WorkoutSummarySetUi = WorkoutSummarySetUi(
    number = number,
    weightKg = weight.toLoadLabel(),
    reps = reps,
    oneRm = oneRm,
    previousOneRm = previous?.oneRm,
)
