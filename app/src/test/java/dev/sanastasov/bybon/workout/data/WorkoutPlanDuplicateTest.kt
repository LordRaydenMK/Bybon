package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.bodyweight.data.WeightDao
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.repdbCatalogCommit
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.upperBodyA
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutPlanDuplicateTest {

    @Test
    fun `duplicatePlan inserts a copy with the same exercises after the source`() = runTest {
        val repository = repository()
        val edited = fullBodyA.copy(
            description = "Heavy day",
            sets = fullBodyA.sets.mapIndexed { index, planned ->
                if (index == 0) {
                    planned.copy(
                        warmupSets = 1,
                        sets = 4,
                        repRange = 5..8,
                        restAfterWorkSet = 90.seconds,
                        notes = listOf("Pause"),
                    )
                } else {
                    planned
                }
            },
        )
        repository.updatePlan(fullBodyA.id) { edited }
        repository.updateWorkout(fullBodyA.toWorkoutSession())

        val copy = repository.duplicatePlan(fullBodyA.id)
        val plans = repository.workoutPlans(WorkoutPlansFilter.AllPlans).first()

        assert(copy.name == "Copy of ${edited.name}")
        assert(copy.id != edited.id)
        assert(copy.description == edited.description)
        assert(copy.sets == edited.sets)
        assert(!copy.isArchived)
        assert(plans.map { it.id } == listOf(edited.id, copy.id, fullBodyB.id, upperBodyA.id))
        assert(plans[0] == edited)
        assert(plans[1] == copy)
        assert(repository.workoutSessions().first().map { it.planId } == listOf(fullBodyA.id))
    }

    @Test
    fun `duplicatePlan assigns a distinct id each time and shifts later plans`() = runTest {
        val repository = repository()

        val first = repository.duplicatePlan(fullBodyA.id)
        val second = repository.duplicatePlan(fullBodyA.id)
        val plans = repository.workoutPlans(WorkoutPlansFilter.AllPlans).first()

        assert(first.id != second.id)
        assert(first.id != fullBodyA.id)
        assert(first.name == "Copy of ${fullBodyA.name}")
        assert(second.name == first.name)
        assert(first.sets == fullBodyA.sets)
        assert(second.sets == fullBodyA.sets)
        assert(
            plans.map { it.id } == listOf(
                fullBodyA.id,
                second.id,
                first.id,
                fullBodyB.id,
                upperBodyA.id,
            ),
        )
    }

    @Test
    fun `duplicatePlan keeps an archived plan archived`() = runTest {
        val repository = repository()

        val copy = repository.duplicatePlan(upperBodyA.id)
        val plans = repository.workoutPlans(WorkoutPlansFilter.AllPlans).first()

        assert(copy.isArchived)
        assert(copy.name == "Copy of ${upperBodyA.name}")
        assert(copy.description == upperBodyA.description)
        assert(copy.sets == upperBodyA.sets)
        assert(plans.map { it.id } == listOf(fullBodyA.id, fullBodyB.id, upperBodyA.id, copy.id))
        assert(
            repository.workoutPlans().first().map { it.id } == listOf(fullBodyA.id, fullBodyB.id),
        )
    }

    @Test
    fun `duplicatePlan rejects a missing plan`() = runTest {
        val repository = repository()

        val error = assertFailsWith<IllegalArgumentException> {
            repository.duplicatePlan(WorkoutPlanId("missing"))
        }
        assert(error.message == "Plan missing does not exist")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun TestScope.repository(
        defaultDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler),
    ): WorkoutsRepository {
        val exerciseDao: ExerciseDao = FakeExerciseDao()
        val workoutPlanDao: WorkoutPlanDao = FakeWorkoutPlanDao()
        val weightDao: WeightDao = FakeWeightDao()
        return WorkoutsRepositoryImpl(
            exerciseDao = exerciseDao,
            workoutPlanDao = workoutPlanDao,
            workoutSessionDao = FakeWorkoutSessionDao(exerciseDao, workoutPlanDao),
            weightDao = weightDao,
            catalog = catalogExercises,
            repdbCommit = repdbCatalogCommit,
            defaultDispatcher = defaultDispatcher,
        )
    }
}
