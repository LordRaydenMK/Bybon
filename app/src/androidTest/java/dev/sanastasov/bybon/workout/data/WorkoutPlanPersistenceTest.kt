package dev.sanastasov.bybon.workout.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sanastasov.bybon.data.BybonDatabase
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.addWorkSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.upperBodyA
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutPlanPersistenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val db = Room.inMemoryDatabaseBuilder<BybonDatabase>(context)
        .setDriver(AndroidSQLiteDriver())
        .build()

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun freshDatabaseSeedsBuiltInPlansAndKeepsUserEditsAcrossInstances() = runBlocking {
        val exerciseDao = db.exerciseDao()
        val planDao = db.workoutPlanDao()
        val repository = WorkoutsRepositoryImpl(exerciseDao, planDao)

        assert(
            repository.workoutPlans().first() == listOf(fullBodyA, fullBodyB),
        )
        assert(
            repository.workoutPlans(WorkoutPlansFilter.AllPlans).first() ==
                listOf(fullBodyA, fullBodyB, upperBodyA),
        )

        repository.updatePlan(fullBodyA.id) { it.addWorkSet("bench-press") }
        repository.archivePlan(fullBodyA.id, archived = true)

        val extra = fullBodyA.copy(id = WorkoutPlanId("extra"), name = "Extra")
        repository.importHistory(
            plans = listOf(extra),
            sessions = emptyList(),
            exercises = emptyList(),
        )

        val relaunched = WorkoutsRepositoryImpl(exerciseDao, planDao)
        val stored = relaunched.workoutPlans(WorkoutPlansFilter.AllPlans).first()
        assert(stored.first().isArchived)
        assert(stored.first().sets.first().sets == 4)
        assert(stored[1] == fullBodyB)
        assert(stored[2] == upperBodyA)
        assert(stored.last() == extra)
        assert(relaunched.workoutPlans().first() == listOf(fullBodyB, extra))
    }
}
