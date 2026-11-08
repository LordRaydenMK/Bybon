package dev.sanastasov.bybon.workout.ui.history

import android.net.Uri
import dev.sanastasov.bybon.domain.isoWeekStart
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.ui.formatElapsedClock
import dev.sanastasov.bybon.workout.ui.toLoadLabel
import dev.sanastasov.bybon.workout.ui.toStartTimeLabel
import java.time.LocalDate
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
        val sections: List<WorkoutHistoryPeriodSection>,
    ) : WorkoutHistoryUiState() {
        val sessions: List<WorkoutSessionHistoryUi>
            get() = sections.flatMap { it.sessions }
    }
}

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

internal fun WorkoutSession.toHistoryUi(): WorkoutSessionHistoryUi {
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
        exercises = exercises.map { it.toTopSetUi() },
    )
}

private fun WorkoutExercise.toTopSetUi(): ExerciseTopSetUi = ExerciseTopSetUi(
    name = exerciseDefinition.name,
    weightKg = topSet.weight.toLoadLabel(exerciseDefinition.equipment),
    reps = topSet.reps,
    oneRm = topSet.oneRm,
)
