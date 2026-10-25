package dev.sanastasov.bybon.workout.data

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.domain.BodyPart
import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.Mechanic
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addWorkSet
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.repdbCatalogCommit
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
        val repository: WorkoutsRepository = repository()

        assert(repository.exercises().first() == catalogExercises)
    }

    @Test
    fun `importHistory appends new exercises to the catalog`() = runTest {
        val repository: WorkoutsRepository = repository()
        val crunch = ExerciseDefinition(
            "crunch-machine",
            "Crunch (Machine)",
            BodyPart.Core,
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
        val repository: WorkoutsRepository = repository()

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
        val repository: WorkoutsRepository = repository()
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
        val repository: WorkoutsRepository = repository()

        repository.archivePlan(upperBodyA.id, archived = false)

        assert(
            repository.workoutPlans().first() ==
                listOf(fullBodyA, fullBodyB, upperBodyA.copy(isArchived = false)),
        )
    }

    @Test
    fun `updatePlan transforms only the matching plan`() = runTest {
        val repository: WorkoutsRepository = repository()

        repository.updatePlan(fullBodyA.id) { it.addWorkSet("bench-press") }

        val updated = repository.workoutPlans().first().first()
        assert(updated.sets.first().sets == 4)
        assert(repository.workoutPlans().first()[1] == fullBodyB)
        assert(
            repository.workoutPlans(WorkoutPlansFilter.AllPlans).first().last() == upperBodyA,
        )
    }

    @Test
    fun `updatePlan fails when the plan does not exist`() = runTest {
        val repository: WorkoutsRepository = repository()

        val thrown = runCatching {
            repository.updatePlan(WorkoutPlanId("missing")) { it }
        }.exceptionOrNull()

        assert(thrown is IllegalArgumentException)
        assert(thrown?.message == "Plan missing does not exist")
        assert(
            repository.workoutPlans(WorkoutPlansFilter.AllPlans).first() ==
                listOf(fullBodyA, fullBodyB, upperBodyA),
        )
    }

    @Test
    fun `deleteWorkout removes the session`() = runTest {
        val repository: WorkoutsRepository = repository()
        val started = fullBodyA.toWorkoutSession()
        repository.updateWorkout(started)

        repository.deleteWorkout(started.id)

        assert(repository.workoutSessions().first().isEmpty())
    }

    @Test
    fun `updateWorkout does not replace an in-progress session with an overview draft`() = runTest {
        val repository: WorkoutsRepository = repository()
        val started = fullBodyA.toWorkoutSession()
        repository.updateWorkout(started)

        repository.updateWorkout(fullBodyA.toOverviewSession())

        assert(repository.workoutSessions().first().single() == started)
    }

    @Test
    fun `updateWorkout does not replace a resting session with an overview draft`() = runTest {
        val repository: WorkoutsRepository = repository()
        val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0)
        val started = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val warmupIndices = started.exercises.first().warmupSets!!.indices
        val resting = warmupIndices.fold(started) { session, index ->
            session.completeSet(
                session.exercises.first(),
                index,
                isWarmup = true,
                now = startedAt.plusMinutes(4),
            )
        }
        repository.updateWorkout(resting)

        repository.updateWorkout(fullBodyA.toOverviewSession())

        assert(repository.workoutSessions().first().single() == resting)
    }

    @Test
    fun `importHistory skips a session that is already stored`() = runTest {
        val repository: WorkoutsRepository = repository()
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
        val repository: WorkoutsRepository = repository()
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
        val repository: WorkoutsRepository = repository()
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
        val repository: WorkoutsRepository = repository()
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
        val upperBodyPlan = fullBodyA.copy(
            id = WorkoutPlanId("upper-body-a"),
            name = "Upper body A",
        )
        val upperBody = completedSession(
            planId = "upper-body-a",
            planName = "Upper body A",
            startedAt = startedAt,
            exercises = listOf(completedExercise("bench-press", 30f to 10)),
        )

        val result = repository.importHistory(
            plans = listOf(upperBodyPlan),
            sessions = listOf(upperBody),
            exercises = emptyList(),
        )

        assert(result.sessions == listOf(upperBody))
        assert(result.sessionsSkipped == 0)
        assert(repository.workoutSessions().first() == listOf(fullBody, upperBody))
    }

    @Test
    fun `importHistory does not crash collectors that see a plan before its exercises`() = runTest {
        val repository: WorkoutsRepository = repository()
        val extra = fullBodyA.copy(id = WorkoutPlanId("extra"), name = "Extra")
        repository.workoutPlans(WorkoutPlansFilter.AllPlans).test {
            awaitItem()
            repository.importHistory(
                plans = listOf(extra),
                sessions = emptyList(),
                exercises = emptyList(),
            )
            assert(expectMostRecentItem().last() == extra)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `importHistory does not insert a plan or exercise id that already exists`() = runTest {
        val repository: WorkoutsRepository = repository()
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

    @Test
    fun `importHistory keeps the first exercise when one file repeats an id`() = runTest {
        val repository = repository()
        val first = ExerciseDefinition(
            "crunch-machine",
            "Crunch (Machine)",
            BodyPart.Core,
            Equipment.Machine,
            Mechanic.Isolation,
        )

        val result = repository.importHistory(
            plans = emptyList(),
            sessions = emptyList(),
            exercises = listOf(first, first.copy(name = "Second")),
        )

        assert(result.exercises == listOf(first))
        assert(repository.exercises().first().last() == first)
        assert(repository.exercises().first().count { it.id == first.id } == 1)
    }

    @Test
    fun `reading plans seeds the shipped catalog once for this repdb commit`() = runTest {
        val dao = FakeExerciseDao()
        val repository = repository(dao, repdbCommit = "commit-a")

        repository.workoutPlans().first()

        assert(dao.catalogCommit() == "commit-a")
        assert(dao.observeAll().first().map { it.toDefinition() } == catalogExercises)
        assert(repository.exercises().first() == catalogExercises)
    }

    @Test
    fun `the same repdb commit does not overwrite shipped rows`() = runTest {
        val dao = FakeExerciseDao()
        val bench = catalogExercises.first { it.id == "bench-press" }
        val seeded = repository(dao, repdbCommit = "commit-a")
        seeded.exercises().first()
        val local = bench.toEntity(source = ExerciseSource.SHIPPED).copy(name = "Local Name")
        dao.upsertAll(listOf(local))

        val again = repository(
            dao,
            catalog = catalogExercises.map { exercise ->
                if (exercise.id == bench.id) exercise.copy(name = "Updated Bench") else exercise
            },
            repdbCommit = "commit-a",
        )

        assert(again.exercises().first().single { it.id == bench.id }.name == "Local Name")
    }

    @Test
    fun `catalog refresh updates shipped rows and leaves import and user rows`() = runTest {
        val dao = FakeExerciseDao()
        val bench = catalogExercises.first { it.id == "bench-press" }
        val imported = ExerciseDefinition(
            "crunch-machine",
            "Crunch (Machine)",
            BodyPart.Core,
            Equipment.Machine,
            Mechanic.Isolation,
        )
        val user = ExerciseDefinition(id = "my-curl", name = "My Curl")
        dao.upsertAll(listOf(user.toEntity(source = ExerciseSource.USER, isArchived = true)))
        val seeded = repository(dao, repdbCommit = "commit-a")
        seeded.importHistory(
            plans = emptyList(),
            sessions = emptyList(),
            exercises = listOf(imported),
        )

        val refreshed = repository(
            dao,
            catalog = catalogExercises.map { exercise ->
                when (exercise.id) {
                    bench.id -> exercise.copy(name = "Updated Bench")
                    else -> exercise
                }
            } + imported.copy(name = "Shipped Crunch") + user.copy(name = "Shipped Curl"),
            repdbCommit = "commit-b",
        )

        val stored = refreshed.exercises().first()
        assert(stored.single { it.id == bench.id }.name == "Updated Bench")
        assert(stored.single { it.id == imported.id } == imported)
        assert(stored.single { it.id == user.id } == user)
        val entities = dao.observeAll().first()
        assert(entities.single { it.id == bench.id }.source == ExerciseSource.SHIPPED)
        assert(entities.single { it.id == bench.id }.isArchived == false)
        assert(entities.single { it.id == imported.id }.source == ExerciseSource.IMPORT)
        assert(entities.single { it.id == user.id }.source == ExerciseSource.USER)
        assert(entities.single { it.id == user.id }.isArchived)
        assert(dao.catalogCommit() == "commit-b")
    }

    @Test
    fun `completed history and an in-progress session survive a new repository instance`() =
        runTest {
            val exerciseDao = FakeExerciseDao()
            val planDao = FakeWorkoutPlanDao()
            val sessionDao = FakeWorkoutSessionDao(exerciseDao, planDao)
            val first = repository(exerciseDao, planDao, sessionDao)
            val history = completedSession(
                planId = "full-body-a",
                planName = "Full Body A",
                startedAt = LocalDateTime.of(2026, 8, 20, 17, 59, 34),
                exercises = listOf(completedExercise("bench-press", 80f to 8)),
            )
            val live = fullBodyA.toWorkoutSession(
                startedAt = LocalDateTime.of(2026, 8, 21, 18, 0, 0),
            )
            first.importHistory(
                plans = emptyList(),
                sessions = listOf(history),
                exercises = emptyList(),
            )
            first.updateWorkout(live)

            val second = repository(exerciseDao, planDao, sessionDao)

            assert(second.workoutSessions().first() == listOf(history, live))
        }

    @Test
    fun `edited and archived plans survive a new repository instance`() = runTest {
        val exerciseDao = FakeExerciseDao()
        val planDao = FakeWorkoutPlanDao()
        val first = repository(exerciseDao, planDao)

        first.updatePlan(fullBodyA.id) { it.addWorkSet("bench-press") }
        first.archivePlan(fullBodyA.id, archived = true)

        val second = repository(exerciseDao, planDao)
        val stored = second.workoutPlans(WorkoutPlansFilter.AllPlans).first()

        assert(stored.first().isArchived)
        assert(stored.first().sets.first().sets == 4)
        assert(stored[1] == fullBodyB)
        assert(stored.last() == upperBodyA)
        assert(second.workoutPlans().first() == listOf(fullBodyB))
    }

    private fun repository(
        exerciseDao: ExerciseDao = FakeExerciseDao(),
        workoutPlanDao: WorkoutPlanDao = FakeWorkoutPlanDao(),
        workoutSessionDao: WorkoutSessionDao = FakeWorkoutSessionDao(exerciseDao, workoutPlanDao),
        catalog: List<ExerciseDefinition> = catalogExercises,
        repdbCommit: String = repdbCatalogCommit,
    ): WorkoutsRepository = WorkoutsRepositoryImpl(
        exerciseDao = exerciseDao,
        workoutPlanDao = workoutPlanDao,
        workoutSessionDao = workoutSessionDao,
        catalog = catalog,
        repdbCommit = repdbCommit,
    )
}
