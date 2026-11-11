package dev.sanastasov.bybon.workout.ui.summary

import app.cash.turbine.test
import dev.sanastasov.bybon.domain.toDisplayDate
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.domain.exercisePerformance
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.workoutSessionSummary
import dev.sanastasov.bybon.workout.ui.details.toOneRmTrend
import dev.sanastasov.bybon.workout.ui.details.toPersonalRecordUi
import dev.sanastasov.bybon.workout.ui.toStartTimeLabel
import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
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
                    "romanian-deadlift",
                    45f to 12,
                    45f to 11,
                ),
                completedExercise(
                    "incline-db-press",
                    20f to 13,
                    20f to 11,
                    20f to 8,
                ),
            ),
        )
        val viewModel = summaryViewModel(session)

        val expected = WorkoutSummaryUiState.Content(
            title = "Full Body B",
            date = "13 Aug 2026",
            startTime = "18:00",
            duration = "40m",
            sessionSummary = session.sessionSummaryUi(),
            exercises = listOf(
                WorkoutSummaryExerciseUi(
                    id = "romanian-deadlift",
                    name = "Romanian Deadlift",
                    sets = listOf(
                        WorkoutSummarySetUi(1, "45", 12, oneRm(45f, 12)),
                        WorkoutSummarySetUi(2, "45", 11, oneRm(45f, 11)),
                    ),
                    personalRecord = session.personalRecordUi("romanian-deadlift"),
                    oneRmTrend = session.oneRmTrend("romanian-deadlift"),
                    volumeLoadLabel = "1035 kg",
                ),
                WorkoutSummaryExerciseUi(
                    id = "incline-db-press",
                    name = "Incline Dumbbell Press",
                    sets = listOf(
                        WorkoutSummarySetUi(1, "20", 13, oneRm(20f, 13)),
                        WorkoutSummarySetUi(2, "20", 11, oneRm(20f, 11)),
                        WorkoutSummarySetUi(3, "20", 8, oneRm(20f, 8)),
                    ),
                    personalRecord = session.personalRecordUi("incline-db-press"),
                    oneRmTrend = session.oneRmTrend("incline-db-press"),
                    volumeLoadLabel = "640 kg",
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
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12)),
        )
        val other = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(completedExercise("bench-press", 80f to 8)),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(other, matching))
        val viewModel = WorkoutSummaryViewModel(matching.id, repository, backgroundScope)

        viewModel.uiState.test {
            skipItems(1)
            val actual = awaitItem() as WorkoutSummaryUiState.Content
            assert(actual.title == "Full Body B")
            assert(actual.date == "13 Aug 2026")
            assert(actual.startTime == "18:00")
            assert(actual.duration == "40m")
            assert(actual.exercises.single().id == "romanian-deadlift")
        }
    }

    @Test
    fun `ui state updates when the repository emits a new version of the session`() = runTest {
        val original = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12)),
        )
        val updated = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 50f to 10)),
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

    @Test
    fun `summary shows date start time and duration from the completed session`() {
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 25, 54),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12)),
        ).copy(duration = 47.minutes + 44.seconds)

        val content = session.toSummaryUi()
        assert(content.date == "13 Aug 2026")
        assert(content.startTime == "18:25")
        assert(content.duration == "47m")
        assert(content.sessionSummary.subtitle == "Full Body B · 13 Aug 2026 · Start 18:25")
        assert(content.sessionSummary.duration == "47m")
    }

    @Test
    fun `summary rejects a session that is not completed`() {
        val session = fullBodyA.toWorkoutSession(
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
        )

        val error = assertFailsWith<IllegalStateException> { session.toSummaryUi() }
        assert(error.message == "Summary UI requires a completed session, was ${session.state}")
    }

    @Test
    fun `summary uses dash when a set has no load`() {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                WorkoutExercise(
                    exerciseDefinition = catalogExercise("assisted-pull-ups"),
                    repRange = 6..10,
                    sets = listOf(
                        ExerciseSet(
                            catalogExercise("assisted-pull-ups"),
                            null,
                            8,
                            SetState.Completed(),
                        ),
                    ),
                ),
            ),
        )

        val content = session.toSummaryUi()
        assert(content.exercises.single().sets.single().weightKg == null)
        assert(content.exercises.single().sets.single().reps == 8)
        assert(content.exercises.single().sets.single().oneRm == null)
        assert(content.exercises.single().sets.single().previousOneRm == null)
    }

    @Test
    fun `summary work sets include previous 1RM`() {
        val previous = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 6, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 9)),
        )
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12)),
        )

        val set = listOf(previous, session).toSummarySets(session)
        assert(set.single().oneRm == oneRm(45f, 12))
        assert(set.single().previousOneRm == oneRm(45f, 9))
    }

    @Test
    fun `summary pairs work sets when warmup counts differ`() {
        val previous = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 3, 18, 0),
            exercises = listOf(
                completedExercise("bench-press", 80f to 8, 80f to 7).copy(
                    warmupSets = listOf(
                        ExerciseSet(
                            catalogExercise("bench-press"),
                            Weight.kilograms(40),
                            5,
                            SetState.Completed(),
                        ),
                    ),
                ),
            ),
        )
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                completedExercise("bench-press", 85f to 8, 85f to 6).copy(
                    warmupSets = listOf(
                        ExerciseSet(
                            catalogExercise("bench-press"),
                            Weight.kilograms(45),
                            5,
                            SetState.Completed(),
                        ),
                        ExerciseSet(
                            catalogExercise("bench-press"),
                            Weight.kilograms(55),
                            3,
                            SetState.Completed(),
                        ),
                    ),
                ),
            ),
        )

        val work = listOf(previous, session).toSummarySets(session).filterNot { it.isWarmup }
        assert(work.map { it.previousOneRm } == listOf(oneRm(80f, 8), oneRm(80f, 7)))
    }

    @Test
    fun `summary compares overlapping work sets when counts differ`() {
        val previous = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 6, 18, 0),
            exercises = listOf(
                completedExercise("romanian-deadlift", 40f to 10, 40f to 9, 40f to 8),
            ),
        )
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12, 45f to 11)),
        )

        val work = listOf(previous, session).toSummarySets(session)
        assert(work.map { it.previousOneRm } == listOf(oneRm(40f, 10), oneRm(40f, 9)))
    }

    @Test
    fun `summary incline session reports e1RM vs previous workout`() {
        val previous = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 25),
            exercises = listOf(completedExercise("incline-db-press", 20f to 13)),
        )
        val session = completedSession(
            planId = "upper-body-b",
            planName = "Upper body B",
            startedAt = LocalDateTime.of(2026, 8, 20, 17, 59),
            exercises = listOf(completedExercise("incline-db-press", 20f to 14)),
        )

        val exercise = listOf(previous, session).toSummaryExercise(session)
        assert(exercise.previous?.workoutLabel == "Previous workout: 13 Aug 2026 · 7 days ago")
        assert(exercise.topSetOneRm == oneRm(20f, 14))
        assert(exercise.averageOneRm == oneRm(20f, 14))
        assert(exercise.previous?.topSetOneRm == oneRm(20f, 13))
        assert(exercise.previous?.averageOneRm == oneRm(20f, 13))
        assert(exercise.volumeLoadLabel == "280 kg")
        assert(exercise.sets.single().contentDescription() == "1. 20 kg × 14, 29.33 kg e1RM")
    }

    @Test
    fun `summary volume ignores blank load and averages working-set e1RM`() {
        val previous = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 11, 6, 18, 0),
            exercises = listOf(
                completedExercise("incline-db-press", 17.5f to 14, 17.5f to 12, 17.5f to 10),
            ),
        )
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 11, 10, 18, 0),
            exercises = listOf(
                completedExercise(
                    "incline-db-press",
                    17.5f to 15,
                    17.5f to 13,
                    17.5f to 11,
                ).copy(
                    warmupSets = listOf(
                        ExerciseSet(
                            catalogExercise("incline-db-press"),
                            Weight.kilograms(10),
                            8,
                            SetState.Completed(),
                        ),
                        ExerciseSet(
                            catalogExercise("incline-db-press"),
                            null,
                            3,
                            SetState.Completed(),
                        ),
                    ),
                ),
            ),
        )

        val exercise = listOf(previous, session).toSummaryExercise(session, now = session.startedAt)
        assert(exercise.previous?.workoutLabel == "Previous workout: 6 Nov 2026 · 4 days ago")
        assert(exercise.workSets.size == 3)
        assert(exercise.totalReps == 39)
        assert(exercise.volumeLoadLabel == "682.5 kg")
        assert(exercise.topSetOneRm == oneRm(17.5f, 15))
        val expectedAverage = listOf(
            oneRm(17.5f, 15),
            oneRm(17.5f, 13),
            oneRm(17.5f, 11),
        ).map { it.kilogramsValue.toDouble() }.average()
        assert(exercise.averageOneRm == Weight.kilograms(expectedAverage.toFloat()))
        assert(exercise.previous?.topSetOneRm == oneRm(17.5f, 14))

        val summary = listOf(previous, session).let { sessions ->
            session.toSummaryUi(sessions, session.startedAt).sessionSummary
        }
        assert(summary.workingSetCount == 3)
        assert(summary.totalReps == 39)
        assert(summary.volumeLoadLabel == "682.5 kg")
        assert(summary.setPerformance == SetPerformanceUi(3, 0, 0))
        assert(summary.exercisePerformance.single().name == "Incline Dumbbell Press")
        assert(summary.exercisePerformance.single().changeLabel.startsWith("↑"))
    }

    @Test
    fun `summary rematches imported sessions that stored no previous`() {
        val previous = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 6, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 40f to 10, 40f to 8)),
        )
        val imported = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12, 45f to 9)),
        )
        assert(imported.exercises.single().sets.all { it.previous == null })

        val work = listOf(previous, imported).toSummarySets(imported)
        assert(work.map { it.previousOneRm } == listOf(oneRm(40f, 10), oneRm(40f, 8)))
    }

    @Test
    fun `summary lists warmups before numbered work sets and omits warmup 1RM`() {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                completedExercise("bench-press", 80f to 8, 80f to 7).copy(
                    warmupSets = listOf(
                        ExerciseSet(
                            exerciseDefinition = catalogExercise("bench-press"),
                            weight = Weight.kilograms(40),
                            reps = 5,
                            setState = SetState.Completed(),
                        ),
                        ExerciseSet(
                            exerciseDefinition = catalogExercise("bench-press"),
                            weight = null,
                            reps = 6,
                            setState = SetState.Completed(),
                        ),
                    ),
                ),
            ),
        )

        val sets = session.toSummaryUi().exercises.single().sets
        assert(
            sets == listOf(
                WorkoutSummarySetUi(null, "40", 5, null, isWarmup = true),
                WorkoutSummarySetUi(null, null, 6, null, isWarmup = true),
                WorkoutSummarySetUi(1, "80", 8, oneRm(80f, 8)),
                WorkoutSummarySetUi(2, "80", 7, oneRm(80f, 7)),
            ),
        )
    }

    @Test
    fun `session and exercise notes are shown in the summary`() = runTest {
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(
                completedExercise("romanian-deadlift", 45f to 12),
                completedExercise(
                    "incline-db-press",
                    20f to 13,
                    notes = listOf("Rep range 11-15", "Pause at the bottom"),
                ),
            ),
            note = "Friday full body workout",
        )
        val viewModel = summaryViewModel(session)

        viewModel.uiState.test {
            skipItems(1)
            val actual = awaitItem() as WorkoutSummaryUiState.Content
            assert(actual.note == "Friday full body workout")
            assert(actual.exercises.first().notes.isEmpty())
            assert(
                actual.exercises.last().notes ==
                    listOf("Rep range 11-15", "Pause at the bottom"),
            )
        }
    }

    @Test
    fun `opening details emits the exercise id`() = runTest {
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12)),
        )
        val viewModel = summaryViewModel(session)

        viewModel.onAction(WorkoutSummaryAction.OnOpenExerciseDetails("romanian-deadlift"))

        assert(
            viewModel.effects.first() ==
                WorkoutSummaryEffect.OpenExerciseDetails("romanian-deadlift"),
        )
    }

    @Test
    fun `deleting a session removes it and navigates back`() = runTest {
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12)),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(session))
        val viewModel = WorkoutSummaryViewModel(session.id, repository, backgroundScope)

        viewModel.effects.test {
            viewModel.uiState.first { it is WorkoutSummaryUiState.Content }
            viewModel.onAction(WorkoutSummaryAction.OnDeleteSession)
            assert(awaitItem() == WorkoutSummaryEffect.NavigateBack)
        }
        assert(repository.workoutSessions().first().isEmpty())
    }

    @Test
    fun `summary shows all-time 1RM and last 3 months top-set 1RM trend`() = runTest {
        val now = LocalDateTime.of(2026, 10, 22, 18, 0)
        val olderPr = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 6, 1, 18, 0),
            exercises = listOf(completedExercise("bench-press", 90f to 5)),
        )
        val earlierRecent = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(completedExercise("bench-press", 70f to 8)),
        )
        val summarySession = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 10, 1, 18, 0),
            exercises = listOf(
                completedExercise("bench-press", 80f to 8),
                WorkoutExercise(
                    exerciseDefinition = catalogExercise("assisted-pull-ups"),
                    repRange = 6..10,
                    sets = listOf(
                        ExerciseSet(
                            catalogExercise("assisted-pull-ups"),
                            null,
                            8,
                            SetState.Completed(),
                        ),
                    ),
                ),
            ),
        )
        val inProgress = fullBodyA.toWorkoutSession(startedAt = now.minusDays(1))
        val completed = listOf(olderPr, earlierRecent, summarySession)
        val viewModel = WorkoutSummaryViewModel(
            summarySession.id,
            FakeWorkoutsRepository(initialSessions = completed + inProgress),
            backgroundScope,
            now = { now },
        )

        viewModel.uiState.test {
            skipItems(1)
            val actual = awaitItem() as WorkoutSummaryUiState.Content
            val bench = actual.exercises.first()
            val pullUp = actual.exercises.last()
            val expectedPerformance = completed.exercisePerformance("bench-press", now)
            assert(bench.personalRecord == expectedPerformance.personalRecord?.toPersonalRecordUi())
            assert(bench.personalRecord?.dateLabel == "1 Jun 2026")
            assert(bench.oneRmTrend == expectedPerformance.recentSessions.toOneRmTrend())
            assert(bench.oneRmTrend.map { it.dateLabel } == listOf("10 Aug", "1 Oct"))
            assert(pullUp.personalRecord == null)
            assert(pullUp.oneRmTrend.isEmpty())
        }
    }

    private fun List<WorkoutSession>.toSummarySets(
        session: WorkoutSession,
    ): List<WorkoutSummarySetUi> = toSummaryExercise(session).sets

    private fun List<WorkoutSession>.toSummaryExercise(
        session: WorkoutSession,
        now: LocalDateTime = session.startedAt,
    ): WorkoutSummaryExerciseUi = session.toSummaryUi(this, now).exercises.single()

    private fun oneRm(kg: Float, reps: Int): Weight = Weight.kilograms(estimateOneRmKg(kg, reps))

    private fun WorkoutSession.personalRecordUi(exerciseId: String) =
        listOf(this).exercisePerformance(exerciseId, startedAt).personalRecord?.toPersonalRecordUi()

    private fun WorkoutSession.oneRmTrend(exerciseId: String) =
        listOf(this).exercisePerformance(exerciseId, startedAt).recentSessions.toOneRmTrend()

    private fun WorkoutSession.sessionSummaryUi(
        completed: List<WorkoutSession> = listOf(this),
    ): WorkoutSessionSummaryUi {
        val completedState = state
        check(completedState is WorkoutState.Completed)
        return workoutSessionSummary(completed).toUi(
            planName = planName,
            date = startedAt.toLocalDate().toDisplayDate(),
            startTime = startedAt.toStartTimeLabel(),
            duration = completedState.duration,
        )
    }

    private fun TestScope.summaryViewModel(session: WorkoutSession) = WorkoutSummaryViewModel(
        session.id,
        FakeWorkoutsRepository(initialSessions = listOf(session)),
        backgroundScope,
        now = { session.startedAt },
    )
}
