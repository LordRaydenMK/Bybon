package dev.sanastasov.bybon.workout.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sanastasov.bybon.data.BybonDatabase
import dev.sanastasov.bybon.data.OpenSessionIndexCallback
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutSessionPersistenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val db = Room.inMemoryDatabaseBuilder<BybonDatabase>(context)
        .setDriver(AndroidSQLiteDriver())
        .addCallback(OpenSessionIndexCallback())
        .build()

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun completedHistoryAndLiveSessionSurviveANewRepositoryInstance() = runBlocking {
        val repository = WorkoutsRepositoryImpl(
            db.exerciseDao(),
            db.workoutPlanDao(),
            db.workoutSessionDao(),
        )
        val history = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 20, 17, 59, 34),
            exercises = listOf(completedExercise("bench-press", 80f to 8)),
        )
        val live = fullBodyA.toWorkoutSession(
            startedAt = LocalDateTime.of(2026, 8, 21, 18, 0, 0),
        )
        repository.importHistory(
            plans = emptyList(),
            sessions = listOf(history),
            exercises = emptyList(),
        )
        repository.updateWorkout(live)

        val relaunched = WorkoutsRepositoryImpl(
            db.exerciseDao(),
            db.workoutPlanDao(),
            db.workoutSessionDao(),
        )

        assert(relaunched.workoutSessions().first() == listOf(history, live))
    }
}
