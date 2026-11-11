package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.domain.toDisplayDate
import dev.sanastasov.bybon.domain.toDisplayDateDotRelative
import dev.sanastasov.bybon.workout.domain.CompletedExerciseOccurrence
import dev.sanastasov.bybon.workout.domain.ExerciseAverageOneRmChange
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.PerformanceChange
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutSessionSummary
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.exercisePerformance
import dev.sanastasov.bybon.workout.domain.label
import dev.sanastasov.bybon.workout.domain.latestCompletedExerciseOccurrence
import dev.sanastasov.bybon.workout.domain.withPreviousFrom
import dev.sanastasov.bybon.workout.domain.workoutSessionSummary
import dev.sanastasov.bybon.workout.ui.details.ExerciseOneRmPointUi
import dev.sanastasov.bybon.workout.ui.details.PersonalRecordUi
import dev.sanastasov.bybon.workout.ui.details.toOneRmTrend
import dev.sanastasov.bybon.workout.ui.details.toPersonalRecordUi
import dev.sanastasov.bybon.workout.ui.formatOneRmKg
import dev.sanastasov.bybon.workout.ui.formatTrainingTime
import dev.sanastasov.bybon.workout.ui.toLoadLabel
import dev.sanastasov.bybon.workout.ui.toStartTimeLabel
import java.time.LocalDateTime
import kotlin.time.Duration

sealed class WorkoutSummaryUiState {
    data object Loading : WorkoutSummaryUiState()
    data class Content(
        val title: String,
        val date: String,
        val startTime: String,
        val duration: String,
        val sessionSummary: WorkoutSessionSummaryUi,
        val exercises: List<WorkoutSummaryExerciseUi>,
        val note: String? = null,
    ) : WorkoutSummaryUiState()
}

data class WorkoutSessionSummaryUi(
    val subtitle: String,
    val duration: String,
    val exerciseCount: Int,
    val workingSetCount: Int,
    val totalReps: Int,
    val volumeLoadLabel: String,
    val setPerformance: SetPerformanceUi? = null,
    val exercisePerformance: List<ExerciseAverageOneRmUi> = emptyList(),
    val musclesTrained: List<String> = emptyList(),
)

data class SetPerformanceUi(
    val improved: Int,
    val maintained: Int,
    val declined: Int,
)

data class ExerciseAverageOneRmUi(
    val name: String,
    val changeLabel: String,
)

data class WorkoutSummaryExerciseUi(
    val id: String,
    val name: String,
    val sets: List<WorkoutSummarySetUi>,
    val volumeLoadLabel: String,
    val notes: List<String> = emptyList(),
    val personalRecord: PersonalRecordUi? = null,
    val oneRmTrend: List<ExerciseOneRmPointUi> = emptyList(),
    val previous: WorkoutSummaryPreviousUi? = null,
) {
    val workSets: List<WorkoutSummarySetUi> get() = sets.filterNot { it.isWarmup }

    val topSetOneRm: Weight? get() = workSets.mapNotNull { it.oneRm }.maxOrNull()

    val averageOneRm: Weight? get() = workSets.mapNotNull { it.oneRm }.averageWeight()

    val totalReps: Int get() = workSets.sumOf { it.reps }
}

data class WorkoutSummaryPreviousUi(
    val workoutLabel: String,
    val topSetOneRm: Weight,
    val averageOneRm: Weight,
)

data class WorkoutSummarySetUi(
    val number: Int?,
    val weightKg: String?,
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
    val date = startedAt.toLocalDate().toDisplayDate()
    val startTime = startedAt.toStartTimeLabel()
    val duration = completed.duration.formatTrainingTime()
    return WorkoutSummaryUiState.Content(
        title = planName,
        date = date,
        startTime = startTime,
        duration = duration,
        sessionSummary = workoutSessionSummary(completedSessions).toUi(
            planName = planName,
            date = date,
            startTime = startTime,
            duration = completed.duration,
        ),
        exercises = exercises.mapNotNull {
            it.toSummaryExerciseUi(completedSessions, now, startedAt)
        },
        note = note,
    )
}

internal fun WorkoutSessionSummary.toUi(
    planName: String,
    date: String,
    startTime: String,
    duration: Duration,
): WorkoutSessionSummaryUi = WorkoutSessionSummaryUi(
    subtitle = "$planName · $date · Start $startTime",
    duration = duration.formatTrainingTime(),
    exerciseCount = exerciseCount,
    workingSetCount = workingSetCount,
    totalReps = totalReps,
    volumeLoadLabel = "${formatOneRmKg(volumeLoadKg)} kg",
    setPerformance = setPerformance.toUi(),
    exercisePerformance = exercisePerformance.map { it.toUi() },
    musclesTrained = musclesTrained.map { it.label },
)

private fun PerformanceChange.toUi(): SetPerformanceUi? = takeIf { comparableCount > 0 }?.let {
    SetPerformanceUi(it.improved, it.maintained, it.declined)
}

private fun ExerciseAverageOneRmChange.toUi(): ExerciseAverageOneRmUi = ExerciseAverageOneRmUi(
    name = name,
    changeLabel = formatAverageOneRmChange(currentAverage, previousAverage),
)

private fun WorkoutExercise.toSummaryExerciseUi(
    completedSessions: List<WorkoutSession>,
    now: LocalDateTime,
    sessionStartedAt: LocalDateTime,
): WorkoutSummaryExerciseUi? {
    if (sets.isEmpty()) return null
    val previousOccurrence = completedSessions.latestCompletedExerciseOccurrence(
        id,
        before = sessionStartedAt,
    )
    val previous = previousOccurrence?.exercise
    val matched = if (previous != null) withPreviousFrom(previous) else this
    val warmups = matched.warmupSets.orEmpty().map { it.toWarmupSummaryUi() }
    val work = matched.sets.mapIndexed { index, set -> set.toWorkSummaryUi(index + 1) }
    val performance = completedSessions.exercisePerformance(id, now)
    return WorkoutSummaryExerciseUi(
        id = id,
        name = exerciseDefinition.name,
        sets = warmups + work,
        volumeLoadLabel = matched.sets.volumeLoadLabel(),
        notes = notes,
        personalRecord = performance.personalRecord?.toPersonalRecordUi(),
        oneRmTrend = performance.recentSessions.toOneRmTrend(),
        previous = previousOccurrence?.toPreviousUi(now),
    )
}

private fun CompletedExerciseOccurrence.toPreviousUi(
    now: LocalDateTime,
): WorkoutSummaryPreviousUi? {
    val topSetOneRm = exercise.topSet.oneRm
    val averageOneRm = exercise.sets.mapNotNull { it.oneRm }.averageWeight()
    if (topSetOneRm == null || averageOneRm == null) return null
    val date = startedAt.toLocalDate().toDisplayDateDotRelative(now.toLocalDate())
    return WorkoutSummaryPreviousUi(
        workoutLabel = "Previous workout: $date",
        topSetOneRm = topSetOneRm,
        averageOneRm = averageOneRm,
    )
}

private fun List<Weight>.averageWeight(): Weight? {
    if (isEmpty()) return null
    return Weight.kilograms(map { it.kilogramsValue.toDouble() }.average().toFloat())
}

private fun List<ExerciseSet>.volumeLoadLabel(): String {
    val kilograms = sumOf { set ->
        (set.weight?.kilogramsValue ?: 0f).toDouble() * set.reps
    }
    return "${formatOneRmKg(kilograms.toFloat())} kg"
}

private fun ExerciseSet.toWarmupSummaryUi(): WorkoutSummarySetUi = WorkoutSummarySetUi(
    number = null,
    weightKg = weight?.toLoadLabel(exerciseDefinition.equipment),
    reps = reps,
    oneRm = null,
    isWarmup = true,
)

private fun ExerciseSet.toWorkSummaryUi(number: Int): WorkoutSummarySetUi = WorkoutSummarySetUi(
    number = number,
    weightKg = weight?.toLoadLabel(exerciseDefinition.equipment),
    reps = reps,
    oneRm = oneRm,
    previousOneRm = previousOneRm,
)
