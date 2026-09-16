package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.upperBodyA
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutsRepositoryImplTest {

    @Test
    fun `exposes the built-in exercise catalog`() = runTest {
        val repository = WorkoutsRepositoryImpl()

        assert(repository.exercises().first() == catalogExercises)
    }

    @Test
    fun `importHistory appends new exercises to the catalog`() = runTest {
        val repository = WorkoutsRepositoryImpl()
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
    fun `active workoutPlans omit the default archived plan`() = runTest {
        val repository = WorkoutsRepositoryImpl()

        assert(
            repository.workoutPlans(WorkoutPlansFilter.ActivePlans).first() ==
                listOf(fullBodyA, fullBodyB),
        )
        assert(
            repository.workoutPlans(WorkoutPlansFilter.AllPlans).first() ==
                listOf(fullBodyA, fullBodyB, upperBodyA),
        )
    }

    @Test
    fun `archivePlan hides the plan from active workoutPlans`() = runTest {
        val repository = WorkoutsRepositoryImpl()
        val before = repository.workoutPlans(WorkoutPlansFilter.ActivePlans).first()
        val planId = before.first().id

        repository.archivePlan(planId, archived = true)

        assert(
            repository.workoutPlans(WorkoutPlansFilter.ActivePlans).first() == before.drop(1),
        )
        assert(
            repository.workoutPlans(WorkoutPlansFilter.AllPlans).first().first().isArchived,
        )
    }

    @Test
    fun `archivePlan false unarchives a plan`() = runTest {
        val repository = WorkoutsRepositoryImpl()

        repository.archivePlan(upperBodyA.id, archived = false)

        assert(
            repository.workoutPlans(WorkoutPlansFilter.ActivePlans).first() ==
                listOf(fullBodyA, fullBodyB, upperBodyA.copy(isArchived = false)),
        )
    }
}
