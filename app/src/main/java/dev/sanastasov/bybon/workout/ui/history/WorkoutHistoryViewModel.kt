package dev.sanastasov.bybon.workout.ui.history

import android.net.Uri
import dev.sanastasov.bybon.strong.StrongCsvParser
import dev.sanastasov.bybon.strong.StrongImportResult
import dev.sanastasov.bybon.strong.toStrongImport
import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import java.io.IOException
import java.time.DateTimeException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutHistoryViewModel(
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
    private val contentResolverReader: ContentResolverReader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val importPhase = MutableStateFlow<ImportPhase>(ImportPhase.Idle)

    val uiState: StateFlow<WorkoutHistoryUiState> =
        combine(
            repository.workoutSessions(),
            importPhase,
        ) { sessions, phase ->
            when (phase) {
                ImportPhase.Importing -> WorkoutHistoryUiState.Importing
                is ImportPhase.Summary -> WorkoutHistoryUiState.Summary(phase.summary)
                ImportPhase.Idle -> sessions.toUiState()
            }
        }.stateInWhileInForeground(coroutineScope, WorkoutHistoryUiState.Loading)

    fun onAction(action: WorkoutHistoryAction) {
        when (action) {
            is WorkoutHistoryAction.OnCsvSelected -> importCsv(action.uri)
            WorkoutHistoryAction.OnImportDone -> importPhase.value = ImportPhase.Idle
        }
    }

    private fun importCsv(uri: Uri) {
        importPhase.value = ImportPhase.Importing
        coroutineScope.launch {
            try {
                val csv = withContext(ioDispatcher) { contentResolverReader.read(uri) }
                val existingPlans = repository.workoutPlans(WorkoutPlansFilter.AllPlans).first()
                val existingExercises = repository.exercises().first()
                val result = withContext(defaultDispatcher) {
                    StrongCsvParser.parse(csv).toStrongImport(
                        plans = existingPlans,
                        exerciseCatalog = existingExercises,
                    )
                }
                repository.importHistory(result.plans, result.sessionHistory, result.exercises)
                importPhase.value = ImportPhase.Summary(result.toSummaryUi())
            } catch (e: CancellationException) {
                throw e
            } catch (_: IOException) {
                resetImport()
            } catch (_: SecurityException) {
                resetImport()
            } catch (_: IllegalArgumentException) {
                resetImport()
            } catch (_: IllegalStateException) {
                resetImport()
            } catch (_: DateTimeException) {
                resetImport()
            }
        }
    }

    private fun resetImport() {
        importPhase.value = ImportPhase.Idle
    }

    private fun List<WorkoutSession>.toUiState(): WorkoutHistoryUiState {
        val history = toHistoryUi()
        return if (history.isEmpty()) {
            WorkoutHistoryUiState.Empty
        } else {
            WorkoutHistoryUiState.History(history)
        }
    }

    private sealed class ImportPhase {
        data object Idle : ImportPhase()
        data object Importing : ImportPhase()
        data class Summary(
            val summary: ImportSummaryUi,
        ) : ImportPhase()
    }
}

private fun StrongImportResult.toSummaryUi(): ImportSummaryUi {
    val sessionsByPlan = sessionHistory
        .groupingBy { it.planName }
        .eachCount()
        .map { (planName, sessionCount) ->
            PlanSessionCountUi(planName, sessionCount)
        }
    val dates = sessionHistory
        .filter { it.state is WorkoutState.Completed }
        .map { it.startedAt.toLocalDate() }
    return ImportSummaryUi(
        sessionCount = sessionHistory.size,
        sessionsByPlan = sessionsByPlan,
        plansCreatedCount = plans.size,
        exercisesImportedCount = exercises.size,
        firstSessionDate = dates.minOrNull(),
        lastSessionDate = dates.maxOrNull(),
        workingSetCount = sessionHistory.sumOf { session ->
            session.exercises.sumOf { it.sets.size }
        },
    )
}
