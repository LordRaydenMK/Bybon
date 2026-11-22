package dev.sanastasov.bybon.workout.ui.history

import android.net.Uri
import dev.sanastasov.bybon.domain.isoWeekStart
import dev.sanastasov.bybon.domain.toShortDisplayDate
import dev.sanastasov.bybon.workout.domain.LastSevenDaysSummary
import dev.sanastasov.bybon.workout.domain.OneRmChange
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.latestCompletedExercise
import dev.sanastasov.bybon.workout.domain.performanceChange
import dev.sanastasov.bybon.workout.domain.performanceTopSet
import dev.sanastasov.bybon.workout.ui.formatElapsedClock
import dev.sanastasov.bybon.workout.ui.formatTrainingTime
import dev.sanastasov.bybon.workout.ui.loggedLoadLabel
import dev.sanastasov.bybon.workout.ui.toStartTimeLabel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

sealed class WorkoutHistoryUiState {
    data object Loading : WorkoutHistoryUiState()
    data object Empty : WorkoutHistoryUiState()
    data object Importing : WorkoutHistoryUiState()
    data class Summary(
        val summary: ImportSummaryUi,
    ) : WorkoutHistoryUiState()
    data class History(
        val weekSummary: LastSevenDaysSummaryUi,
        val sections: List<WorkoutHistoryPeriodSection>,
    ) : WorkoutHistoryUiState() {
        val sessions: List<WorkoutSessionHistoryUi>
            get() = sections.flatMap { it.sessions }
    }
}

data class LastSevenDaysSummaryUi(
    val dateRange: String,
    val workoutCount: Int,
    val trainingTime: String,
    val workingSetCount: Int,
    val totalWeight: String,
    val performance: LastSevenDaysPerformanceUi?,
    val daysTrained: DaysTrainedUi,
)

data class LastSevenDaysPerformanceUi(
    val improved: Int,
    val maintained: Int,
    val declined: Int,
)

data class ImportSummaryUi(
    val sessionCount: Int,
    val sessionsByPlan: List<PlanSessionCountUi>,
    val plansCreatedCount: Int,
    val plansArchivedCount: Int,
    val exercisesImportedCount: Int,
    val firstSessionDate: LocalDate?,
    val lastSessionDate: LocalDate?,
    val workingSetCount: Int,
    val sessionsSkipped: Int,
)

data class PlanSessionCountUi(
    val planName: String,
    val sessionCount: Int,
)

data class WorkoutSessionHistoryUi(
    val id: WorkoutSessionId,
    val planName: String,
    val date: LocalDate,
    val startTime: String,
    val duration: String,
    val exercises: List<ExerciseTopSetUi>,
)

data class WorkoutHistoryPeriodSection(
    val key: String,
    val title: String,
    val sessions: List<WorkoutSessionHistoryUi>,
)

data class ExerciseTopSetUi(
    val name: String,
    val weightKg: String,
    val reps: Int,
    val oneRm: Weight?,
    val oneRmChange: OneRmChange? = null,
)

sealed class WorkoutHistoryAction {
    data class OnCsvSelected(
        val uri: Uri,
    ) : WorkoutHistoryAction()
    data object OnImportDone : WorkoutHistoryAction()
}

internal fun List<WorkoutSession>.toHistoryUi(): List<WorkoutSessionHistoryUi> {
    val completed = filter { it.state is WorkoutState.Completed }
    return completed
        .sortedByDescending { it.startedAt }
        .map { it.toHistoryUi(completed) }
}

internal fun List<WorkoutSessionHistoryUi>.toHistoryPeriodSections(
    today: LocalDate,
): List<WorkoutHistoryPeriodSection> {
    val weekStart = today.isoWeekStart()
    val weekEnd = weekStart.plusDays(6)
    val thisMonth = YearMonth.from(today)
    val namedMonths = filter { session ->
        session.date !in weekStart..weekEnd && YearMonth.from(session.date) != thisMonth
    }.groupBy { YearMonth.from(it.date) }
        .entries
        .sortedByDescending { it.key }
        .mapNotNull { (yearMonth, sessions) ->
            sessions.toPeriodSection(
                key = "month-${yearMonth.year}-${yearMonth.monthValue}",
                title = yearMonth.historyLabel(today),
            )
        }
    return listOfNotNull(
        filter { it.date in weekStart..weekEnd }.toPeriodSection("this-week", "This Week"),
        filter { session ->
            YearMonth.from(session.date) == thisMonth && session.date !in weekStart..weekEnd
        }.toPeriodSection("this-month", "This Month"),
    ) + namedMonths
}

private fun List<WorkoutSessionHistoryUi>.toPeriodSection(
    key: String,
    title: String,
): WorkoutHistoryPeriodSection? =
    takeIf { any { session -> session.exercises.isNotEmpty() } }?.let { sessions ->
        WorkoutHistoryPeriodSection(key, title, sessions)
    }

private fun YearMonth.historyLabel(today: LocalDate): String {
    val month = month.getDisplayName(TextStyle.FULL, Locale.US)
    return if (year == today.year) month else "$month $year"
}

internal fun LastSevenDaysSummary.toUi(): LastSevenDaysSummaryUi = LastSevenDaysSummaryUi(
    dateRange = formatLastSevenDaysRange(startDate, endDate),
    workoutCount = workoutCount,
    trainingTime = trainingTime.formatTrainingTime(),
    workingSetCount = workingSetCount,
    totalWeight = volumeLoad.toVolumeLoadLabel(),
    performance = performance.takeIf { it.comparableCount > 0 }?.let {
        LastSevenDaysPerformanceUi(it.improved, it.maintained, it.declined)
    },
    daysTrained = daysTrained.toUi(today = endDate),
)

private fun Weight?.toVolumeLoadLabel(): String = "${this?.kilograms ?: "0"} kg"

internal fun formatLastSevenDaysRange(start: LocalDate, end: LocalDate): String {
    val dash = "–"
    return when {
        start.year != end.year ->
            "${start.toShortDisplayDate()} ${start.year}$dash${end.toShortDisplayDate()} ${end.year}"

        start.month != end.month ->
            "${start.toShortDisplayDate()}$dash${end.toShortDisplayDate()} ${end.year}"

        else -> "${start.dayOfMonth}$dash${end.toShortDisplayDate()} ${end.year}"
    }
}

internal fun WorkoutSession.toHistoryUi(
    completedSessions: List<WorkoutSession> = listOf(this),
): WorkoutSessionHistoryUi {
    val completed = state
    check(completed is WorkoutState.Completed) {
        "History UI requires a completed session, was $completed"
    }
    return WorkoutSessionHistoryUi(
        id = id,
        planName = planName,
        date = startedAt.toLocalDate(),
        startTime = startedAt.toStartTimeLabel(),
        duration = completed.duration.formatElapsedClock(),
        exercises = exercises.map { it.toTopSetUi(completedSessions, startedAt) },
    )
}

private fun WorkoutExercise.toTopSetUi(
    completedSessions: List<WorkoutSession>,
    sessionStartedAt: LocalDateTime,
): ExerciseTopSetUi {
    val current = performanceTopSet()
    val previous = completedSessions
        .latestCompletedExercise(id, before = sessionStartedAt)
        ?.performanceTopSet()
    return ExerciseTopSetUi(
        name = exerciseDefinition.name,
        weightKg = current.loggedLoadLabel(),
        reps = current.reps,
        oneRm = current.oneRm,
        oneRmChange = current.performanceChange(previous),
    )
}
