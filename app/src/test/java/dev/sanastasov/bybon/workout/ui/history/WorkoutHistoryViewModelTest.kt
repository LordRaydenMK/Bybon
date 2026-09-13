package dev.sanastasov.bybon.workout.ui.history

import app.cash.turbine.test
import dev.sanastasov.bybon.strong.readStrongBackupSample
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutHistoryViewModelTest {

    @Test
    fun `empty repository emits empty history`() = runTest {
        val repository = FakeWorkoutsRepository()
        val viewModel = historyViewModel(repository)

        viewModel.uiState.test {
            assert(awaitItem() == WorkoutHistoryUiState.Loading)
            assert(awaitItem() == WorkoutHistoryUiState.Empty)
        }
    }

    @Suppress("LongMethod")
    @Test
    fun `completed sessions are shown newest first with top set performance`() = runTest {
        val older = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                completedExercise(
                    "bench-press-bb",
                    80f to 8,
                    80f to 7,
                    80f to 6,
                ),
                completedExercise(
                    "squat-bb",
                    100f to 5,
                    90f to 8,
                ),
            ),
        )
        val newer = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(
                completedExercise(
                    "rdl-bb",
                    45f to 12,
                    45f to 12,
                ),
            ),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(older, newer))
        val viewModel = historyViewModel(repository)

        val expected = WorkoutHistoryUiState.History(
            listOf(
                WorkoutSessionHistoryUi(
                    id = WorkoutSessionId(
                        WorkoutPlanId("full-body-b"),
                        LocalDateTime.of(2026, 8, 13, 18, 0),
                    ),
                    planName = "Full Body B",
                    date = LocalDate.of(2026, 8, 13),
                    exercises = listOf(
                        ExerciseTopSetUi(
                            name = "Romanian Deadlift (RDL) (barbell)",
                            weightKg = "45",
                            reps = 12,
                            oneRm = Weight.kilograms(estimateOneRmKg(45f, 12)),
                        ),
                    ),
                ),
                WorkoutSessionHistoryUi(
                    id = WorkoutSessionId(
                        WorkoutPlanId("full-body-a"),
                        LocalDateTime.of(2026, 8, 10, 18, 0),
                    ),
                    planName = "Full Body A",
                    date = LocalDate.of(2026, 8, 10),
                    exercises = listOf(
                        ExerciseTopSetUi(
                            name = "Bench Press (barbell)",
                            weightKg = "80",
                            reps = 8,
                            oneRm = Weight.kilograms(estimateOneRmKg(80f, 8)),
                        ),
                        ExerciseTopSetUi(
                            name = "Squat (barbell)",
                            weightKg = "100",
                            reps = 5,
                            oneRm = Weight.kilograms(estimateOneRmKg(100f, 5)),
                        ),
                    ),
                ),
            ),
        )
        viewModel.uiState.test {
            assert(awaitItem() == WorkoutHistoryUiState.Loading)
            assert(awaitItem() == expected)
        }
    }

    @Test
    fun `in-progress sessions are excluded from history`() = runTest {
        val completed = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )
        val inProgress = WorkoutSession(
            planId = WorkoutPlanId("full-body-a"),
            planName = "Full Body A",
            planDescription = null,
            exercises = listOf(completedExercise("bench-press-bb", 80f to 8)),
            startedAt = LocalDateTime.of(2026, 8, 14, 18, 0),
            state = WorkoutState.InProgress,
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(inProgress, completed))
        val viewModel = historyViewModel(repository)

        viewModel.uiState.test {
            skipItems(1)
            val actual = awaitItem() as WorkoutHistoryUiState.History
            assert(actual.sessions.size == 1)
            assert(actual.sessions.single().planName == "Full Body B")
            assert(actual.sessions.single().date == LocalDate.of(2026, 8, 13))
        }
    }

    @Test
    fun `history top set ignores warmup sets`() = runTest {
        val bench = completedExercise("bench-press-bb", 80f to 8, 80f to 7).copy(
            warmupSets = listOf(
                ExerciseSet(
                    exerciseDefinition = exercisesMap.getValue("bench-press-bb"),
                    weight = Weight.kilograms(200),
                    reps = 1,
                    setState = SetState.Completed,
                ),
            ),
        )
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(bench),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(session))
        val viewModel = historyViewModel(repository)

        viewModel.uiState.test {
            skipItems(1)
            val actual = awaitItem() as WorkoutHistoryUiState.History
            assert(actual.sessions.single().exercises.single().weightKg == "80")
            assert(actual.sessions.single().exercises.single().reps == 8)
        }
    }

    @Test
    fun `top set is the heaviest completed set even when another set has a higher 1RM`() = runTest {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                completedExercise(
                    "bench-press-bb",
                    50f to 5,
                    45f to 12,
                ),
            ),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(session))
        val viewModel = historyViewModel(repository)

        viewModel.uiState.test {
            skipItems(1)
            val topSet = (awaitItem() as WorkoutHistoryUiState.History)
                .sessions.single().exercises.single()
            assert(topSet.weightKg == "50")
            assert(topSet.reps == 5)
            assert(topSet.oneRm == Weight.kilograms(estimateOneRmKg(50f, 5)))
        }
    }

    @Test
    fun `ui state updates when the repository emits new sessions`() = runTest {
        val repository = FakeWorkoutsRepository()
        val viewModel = historyViewModel(repository)
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )
        val expected = WorkoutHistoryUiState.History(
            listOf(
                WorkoutSessionHistoryUi(
                    id = WorkoutSessionId(
                        WorkoutPlanId("full-body-b"),
                        LocalDateTime.of(2026, 8, 13, 18, 0),
                    ),
                    planName = "Full Body B",
                    date = LocalDate.of(2026, 8, 13),
                    exercises = listOf(
                        ExerciseTopSetUi(
                            name = "Romanian Deadlift (RDL) (barbell)",
                            weightKg = "45",
                            reps = 12,
                            oneRm = Weight.kilograms(estimateOneRmKg(45f, 12)),
                        ),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            assert(awaitItem() == WorkoutHistoryUiState.Loading)
            assert(awaitItem() == WorkoutHistoryUiState.Empty)
            repository.emitSessions(listOf(session))
            assert(awaitItem() == expected)
        }
    }

    @Test
    fun `importing the strong sample csv shows a spinner then a summary`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = historyViewModel(
            repository,
            csv = readStrongBackupSample(javaClass.classLoader),
        )

        viewModel.uiState.test {
            assert(awaitItem() == WorkoutHistoryUiState.Loading)
            assert(awaitItem() == WorkoutHistoryUiState.Empty)

            viewModel.onAction(WorkoutHistoryAction.OnCsvSelected(dummyUri()))

            assert(awaitItem() == WorkoutHistoryUiState.Importing)
            val summary = (awaitItem() as WorkoutHistoryUiState.Summary).summary
            assert(summary.sessionCount == 52)
            assert(
                summary.sessionsByPlan == listOf(
                    PlanSessionCountUi("Full Body B", 23),
                    PlanSessionCountUi("Full Body A", 23),
                    PlanSessionCountUi("Upper body A", 3),
                    PlanSessionCountUi("Upper body B", 3),
                ),
            )
            assert(summary.plansCreatedCount == 2)
            assert(summary.exercisesImportedCount == 1)
            assert(summary.firstSessionDate == LocalDate.of(2026, 2, 17))
            assert(summary.lastSessionDate == LocalDate.of(2026, 8, 20))
            assert(summary.workingSetCount == 854)
        }
    }

    @Test
    fun `done after import shows the imported history newest first`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = historyViewModel(
            repository,
            csv = readStrongBackupSample(javaClass.classLoader),
        )

        viewModel.uiState.test {
            skipItems(2)
            viewModel.onAction(WorkoutHistoryAction.OnCsvSelected(dummyUri()))
            skipItems(1)
            val summary = awaitItem()
            assert(summary is WorkoutHistoryUiState.Summary)

            viewModel.onAction(WorkoutHistoryAction.OnImportDone)

            val history = awaitItem() as WorkoutHistoryUiState.History
            assert(history.sessions.size == 52)
            assert(history.sessions.first().planName == "Upper body B")
            assert(history.sessions.first().date == LocalDate.of(2026, 8, 20))
            assert(history.sessions.any { it.planName == "Full Body B" })
            assert(history.sessions.any { it.planName == "Full Body A" })
        }
    }

    @Test
    fun `invalid csv returns to the empty history state`() = runTest {
        val repository = FakeWorkoutsRepository()
        val viewModel = historyViewModel(repository, csv = "not a strong csv")

        viewModel.uiState.test {
            assert(awaitItem() == WorkoutHistoryUiState.Loading)
            assert(awaitItem() == WorkoutHistoryUiState.Empty)

            viewModel.onAction(WorkoutHistoryAction.OnCsvSelected(dummyUri()))

            assert(awaitItem() == WorkoutHistoryUiState.Importing)
            assert(awaitItem() == WorkoutHistoryUiState.Empty)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.historyViewModel(repository: FakeWorkoutsRepository, csv: String = "") =
        WorkoutHistoryViewModel(
            repository = repository,
            coroutineScope = backgroundScope,
            contentResolverReader = FakeContentResolverReader(csv),
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
            defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
}
