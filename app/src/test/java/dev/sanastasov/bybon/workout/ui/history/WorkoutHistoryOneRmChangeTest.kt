package dev.sanastasov.bybon.workout.ui.history

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.OneRmChange
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.domain.oneRmChange
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutHistoryOneRmChangeTest {

    private val historyToday = LocalDate.of(2026, 11, 5)

    @Test
    fun `top set 1RM change compares with the previous workout of that exercise`() = runTest {
        val first = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 3, 18, 0),
            exercises = listOf(completedExercise("bench-press", 50f to 8)),
        )
        val previous = completedSession(
            planId = "upper-body-a",
            planName = "Upper body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                completedExercise("bench-press", 50f to 8),
                completedExercise("squat", 100f to 5),
            ),
        )
        val current = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 17, 18, 0),
            exercises = listOf(
                completedExercise("bench-press", 52.5f to 8),
                completedExercise("squat", 90f to 5),
                completedExercise("romanian-deadlift", 45f to 12),
            ),
        )
        val repository = FakeWorkoutsRepository(
            initialSessions = listOf(current, first, previous),
        )
        val viewModel = historyViewModel(repository)

        viewModel.uiState.test {
            val sessions = (awaitHistory() as WorkoutHistoryUiState.History).sessions
            val newest = sessions[0].exercises
            assert(
                newest[0].oneRmChange == oneRmChange(
                    Weight.kilograms(estimateOneRmKg(52.5f, 8)),
                    Weight.kilograms(estimateOneRmKg(50f, 8)),
                ),
            )
            assert(newest[0].oneRmChange is OneRmChange.Improved)
            assert(
                newest[1].oneRmChange == oneRmChange(
                    Weight.kilograms(estimateOneRmKg(90f, 5)),
                    Weight.kilograms(estimateOneRmKg(100f, 5)),
                ),
            )
            assert(newest[1].oneRmChange is OneRmChange.Declined)
            assert(newest[2].oneRmChange == null)
            val previousBench = sessions[1].exercises.first { it.name == "Barbell Bench Press" }
            val previousSquat = sessions[1].exercises.first { it.name == "Barbell Back Squat" }
            assert(previousBench.oneRmChange == OneRmChange.Maintained)
            assert(previousSquat.oneRmChange == null)
            assert(sessions[2].exercises.single().oneRmChange == null)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.historyViewModel(repository: FakeWorkoutsRepository) =
        WorkoutHistoryViewModel(
            repository = repository,
            coroutineScope = backgroundScope,
            contentResolverReader = FakeContentResolverReader(),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            today = { historyToday },
        )

    private suspend fun ReceiveTurbine<WorkoutHistoryUiState>.awaitHistory(): WorkoutHistoryUiState {
        val item = awaitItem()
        return if (item == WorkoutHistoryUiState.Loading) awaitItem() else item
    }
}
