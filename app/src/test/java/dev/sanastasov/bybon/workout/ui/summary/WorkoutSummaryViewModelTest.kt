package dev.sanastasov.bybon.workout.ui.summary

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutSummaryViewModelTest {

    @Test
    fun `completed session is shown with every completed set`() = runTest {
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(
                completedExercise(
                    "rdl-bb",
                    45f to 12,
                    45f to 11,
                ),
                completedExercise(
                    "incline-bench-press-db",
                    20f to 13,
                    20f to 11,
                    20f to 8,
                ),
            ),
        )
        val viewModel = summaryViewModel(session)

        val expected = WorkoutSummaryUiState.Content(
            title = "Full Body B",
            exercises = listOf(
                WorkoutSummaryExerciseUi(
                    id = "rdl-bb",
                    name = "Romanian Deadlift (RDL) (barbell)",
                    sets = listOf(
                        WorkoutSummarySetUi(1, "45", 12, oneRm(45f, 12)),
                        WorkoutSummarySetUi(2, "45", 11, oneRm(45f, 11)),
                    ),
                ),
                WorkoutSummaryExerciseUi(
                    id = "incline-bench-press-db",
                    name = "Incline Bench Press (dumbbell)",
                    sets = listOf(
                        WorkoutSummarySetUi(1, "20", 13, oneRm(20f, 13)),
                        WorkoutSummarySetUi(2, "20", 11, oneRm(20f, 11)),
                        WorkoutSummarySetUi(3, "20", 8, oneRm(20f, 8)),
                    ),
                ),
            ),
        )
        viewModel.uiState.test {
            assert(awaitItem() == WorkoutSummaryUiState.Loading)
            assert(awaitItem() == expected)
        }
    }

    @Test
    fun `only the matching completed session is shown`() = runTest {
        val matching = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )
        val other = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(completedExercise("bench-press-bb", 80f to 8)),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(other, matching))
        val viewModel = WorkoutSummaryViewModel(matching.id, repository, backgroundScope)

        viewModel.uiState.test {
            skipItems(1)
            val actual = awaitItem() as WorkoutSummaryUiState.Content
            assert(actual.title == "Full Body B")
            assert(actual.exercises.single().id == "rdl-bb")
        }
    }

    @Test
    fun `unknown session id fails`() {
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )

        assertFailsWith<NoSuchElementException> {
            listOf(session).requireCompletedSummary(
                WorkoutSessionId(WorkoutPlanId("missing"), LocalDateTime.of(2026, 1, 1, 0, 0)),
            )
        }
    }

    @Test
    fun `in-progress sessions are not shown as a summary`() {
        val inProgress = WorkoutSession(
            planId = WorkoutPlanId("full-body-a"),
            planName = "Full Body A",
            planDescription = null,
            exercises = listOf(completedExercise("bench-press-bb", 80f to 8)),
            startedAt = LocalDateTime.of(2026, 8, 14, 18, 0),
            state = WorkoutState.InProgress,
        )

        assertFailsWith<NoSuchElementException> {
            listOf(inProgress).requireCompletedSummary(inProgress.id)
        }
    }

    @Test
    fun `ui state updates when the repository emits a new version of the session`() = runTest {
        val original = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )
        val updated = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 50f to 10)),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(original))
        val viewModel = WorkoutSummaryViewModel(original.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == WorkoutSummaryUiState.Loading)
            assert(awaitItem() is WorkoutSummaryUiState.Content)
            repository.emitSessions(listOf(updated))
            val content = awaitItem() as WorkoutSummaryUiState.Content
            assert(content.exercises.single().sets.single().weightKg == "50")
            assert(content.exercises.single().sets.single().reps == 10)
        }
    }

    private fun oneRm(kg: Float, reps: Int): Weight = Weight.kilograms(estimateOneRmKg(kg, reps))

    private fun TestScope.summaryViewModel(session: WorkoutSession) = WorkoutSummaryViewModel(
        session.id,
        FakeWorkoutsRepository(initialSessions = listOf(session)),
        backgroundScope,
    )
}
