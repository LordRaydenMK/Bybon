package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addWorkSet
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.upperBodyA
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutsRepositoryImplTest {

    @Test
    fun `exposes the built-in exercise catalog`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()

        assert(repository.exercises().first() == catalogExercises)
    }

    @Test
    fun `importHistory appends new exercises to the catalog`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val crunch = ExerciseDefinition(
            "crunch-machine",
            "Crunch (Machine)",
            MuscleGroup.Core,
            Equipment.Machine,
        )

        repository.importHistory(
            plans = emptyList(),
            sessions = emptyList(),
            exercises = listOf(crunch),
        )

        val stored = repository.exercises().first()
        assert(stored.containsAll(catalogExercises))
        assert(stored.last() == crunch)
    }

    @Test
    fun `default workoutPlans omit the archived plan`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()

        assert(
            repository.workoutPlans().first() ==
                listOf(fullBodyA, fullBodyB),
        )
        assert(
            repository.workoutPlans(WorkoutPlansFilter.AllPlans).first() ==
                listOf(fullBodyA, fullBodyB, upperBodyA),
        )
    }

    @Test
    fun `archivePlan hides the plan from default workoutPlans`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val before = repository.workoutPlans().first()
        val planId = before.first().id

        repository.archivePlan(planId, archived = true)

        assert(
            repository.workoutPlans().first() == before.drop(1),
        )
        assert(
            repository.workoutPlans(WorkoutPlansFilter.AllPlans).first().first().isArchived,
        )
    }

    @Test
    fun `archivePlan false unarchives a plan`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()

        repository.archivePlan(upperBodyA.id, archived = false)

        assert(
            repository.workoutPlans().first() ==
                listOf(fullBodyA, fullBodyB, upperBodyA.copy(isArchived = false)),
        )
    }

    @Test
    fun `updatePlan transforms only the matching plan`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()

        repository.updatePlan(fullBodyA.id) { it.addWorkSet("bench-press-bb") }

        val updated = repository.workoutPlans().first().first()
        assert(updated.sets.first().sets == 4)
        assert(repository.workoutPlans().first()[1] == fullBodyB)
        assert(
            repository.workoutPlans(WorkoutPlansFilter.AllPlans).first().last() == upperBodyA,
        )
    }

    @Test
    fun `updateWorkout does not replace an in-progress session with an overview draft`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val started = fullBodyA.toWorkoutSession()
        repository.updateWorkout(started)

        repository.updateWorkout(fullBodyA.toOverviewSession())

        assert(repository.workoutSessions().first().single() == started)
    }
}
