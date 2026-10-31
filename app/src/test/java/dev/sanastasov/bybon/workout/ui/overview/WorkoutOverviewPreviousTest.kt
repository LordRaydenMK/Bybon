package dev.sanastasov.bybon.workout.ui.overview

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutOverviewPreviousTest {

    @Test
    fun `overview added work set picks up the matching previous work set`() = runTest {
        val previous = fullBodyA.toWorkoutSession().let { session ->
            session.copy(
                exercises = session.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.mapIndexed { index, set ->
                            set.copy(
                                weight = Weight.kilograms(80 + index),
                                reps = 8 + index,
                                setState = SetState.Completed(),
                            )
                        } + ExerciseSet(
                            exercise.exerciseDefinition,
                            Weight.kilograms(83),
                            11,
                            SetState.Completed(),
                        ),
                        warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                            set.copy(
                                weight = Weight.kilograms(20 + index),
                                reps = 8,
                                setState = SetState.Completed(),
                            )
                        },
                    )
                },
                startedAt = LocalDateTime.of(2026, 10, 1, 12, 0),
                duration = Duration.ZERO,
            )
        }
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(previous),
        )
        val viewModel = WorkoutOverviewViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            val draft = awaitItem()!!
            viewModel.onAction(WorkoutOverviewAction.OnAddSet(draft.exercises.first()))
            val added = awaitItem()!!
            assert(added.exercises.first().sets.size == 4)
            assert(
                added.exercises.first().sets.last().previous ==
                    PreviousSetPerformance(Weight.kilograms(83), 11),
            )
        }
    }
}
