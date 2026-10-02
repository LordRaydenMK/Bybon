package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.Mechanic
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addWorkSet
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.domain.upperBodyA
import java.time.LocalDateTime
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
            Mechanic.Isolation,
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

        repository.updatePlan(fullBodyA.id) { it.addWorkSet("bench-press") }

        val updated = repository.workoutPlans().first().first()
        assert(updated.sets.first().sets == 4)
        assert(repository.workoutPlans().first()[1] == fullBodyB)
        assert(
            repository.workoutPlans(WorkoutPlansFilter.AllPlans).first().last() == upperBodyA,
        )
    }

    @Test
    fun `deleteWorkout removes the session`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val started = fullBodyA.toWorkoutSession()
        repository.updateWorkout(started)

        repository.deleteWorkout(started.id)

        assert(repository.workoutSessions().first().isEmpty())
    }

    @Test
    fun `updateWorkout does not replace an in-progress session with an overview draft`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val started = fullBodyA.toWorkoutSession()
        repository.updateWorkout(started)

        repository.updateWorkout(fullBodyA.toOverviewSession())

        assert(repository.workoutSessions().first().single() == started)
    }

    @Test
    fun `importHistory skips a session that is already stored`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 20, 17, 59, 34),
            exercises = listOf(completedExercise("bench-press", 80f to 8)),
            note = "kept",
        )

        val first = repository.importHistory(
            plans = emptyList(),
            sessions = listOf(session),
            exercises = emptyList(),
        )
        val second = repository.importHistory(
            plans = emptyList(),
            sessions = listOf(session.copy(note = "from csv")),
            exercises = emptyList(),
        )

        assert(first.sessions == listOf(session))
        assert(first.sessionsSkipped == 0)
        assert(second.sessions.isEmpty())
        assert(second.sessionsSkipped == 1)
        assert(repository.workoutSessions().first() == listOf(session))
    }

    @Test
    fun `importHistory appends only the session whose id is new`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val startedAt = LocalDateTime.of(2026, 8, 20, 17, 59, 34)
        val kept = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = startedAt,
            exercises = listOf(completedExercise("bench-press", 80f to 8)),
            note = "kept",
        )
        repository.importHistory(
            plans = emptyList(),
            sessions = listOf(kept),
            exercises = emptyList(),
        )
        val fresh = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = startedAt.plusDays(1),
            exercises = listOf(completedExercise("romanian-deadlift", 45f to 12)),
        )

        val result = repository.importHistory(
            plans = emptyList(),
            sessions = listOf(kept.copy(note = "from csv"), fresh),
            exercises = emptyList(),
        )

        assert(result.sessions == listOf(fresh))
        assert(result.sessionsSkipped == 1)
        assert(repository.workoutSessions().first() == listOf(kept, fresh))
    }

    @Test
    fun `importHistory keeps the first session when one file repeats an id`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val first = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 20, 17, 59, 34),
            exercises = listOf(completedExercise("bench-press", 80f to 8)),
            note = "first",
        )

        val result = repository.importHistory(
            plans = emptyList(),
            sessions = listOf(first, first.copy(note = "second")),
            exercises = emptyList(),
        )

        assert(result.sessions == listOf(first))
        assert(result.sessionsSkipped == 1)
        assert(repository.workoutSessions().first() == listOf(first))
    }

    @Test
    fun `importHistory keeps sessions that share a start time on different plans`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val startedAt = LocalDateTime.of(2026, 8, 20, 17, 59, 34)
        val fullBody = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = startedAt,
            exercises = listOf(completedExercise("bench-press", 80f to 8)),
        )
        repository.importHistory(
            plans = emptyList(),
            sessions = listOf(fullBody),
            exercises = emptyList(),
        )
        val upperBody = completedSession(
            planId = "upper-body-a",
            planName = "Upper body A",
            startedAt = startedAt,
            exercises = listOf(completedExercise("bench-press", 30f to 10)),
        )

        val result = repository.importHistory(
            plans = emptyList(),
            sessions = listOf(upperBody),
            exercises = emptyList(),
        )

        assert(result.sessions == listOf(upperBody))
        assert(result.sessionsSkipped == 0)
        assert(repository.workoutSessions().first() == listOf(fullBody, upperBody))
    }

    @Test
    fun `importHistory does not insert a plan or exercise id that already exists`() = runTest {
        val repository: WorkoutsRepository = WorkoutsRepositoryImpl()
        val extra = fullBodyA.copy(id = WorkoutPlanId("extra"), name = "Extra")
        val created = repository.importHistory(
            plans = listOf(extra),
            sessions = emptyList(),
            exercises = emptyList(),
        )
        val bench = catalogExercises.first { it.id == "bench-press" }

        val again = repository.importHistory(
            plans = listOf(extra.copy(name = "Renamed")),
            sessions = emptyList(),
            exercises = listOf(bench.copy(name = "Other")),
        )

        assert(created.plans == listOf(extra))
        assert(again.plans.isEmpty())
        assert(again.exercises.isEmpty())
        assert(repository.workoutPlans(WorkoutPlansFilter.AllPlans).first().last() == extra)
        assert(repository.exercises().first().single { it.id == bench.id } == bench)
    }
}
