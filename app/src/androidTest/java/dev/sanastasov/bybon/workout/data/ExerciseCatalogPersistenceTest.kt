package dev.sanastasov.bybon.workout.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sanastasov.bybon.data.BybonDatabase
import dev.sanastasov.bybon.workout.domain.BodyPart
import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.Mechanic
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.repdbCatalogCommit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseCatalogPersistenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val db = Room.inMemoryDatabaseBuilder<BybonDatabase>(context)
        .setDriver(AndroidSQLiteDriver())
        .build()

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun freshDatabaseSeedsTheShippedCatalog() = runBlocking {
        val dao = db.exerciseDao()
        val repository = WorkoutsRepositoryImpl(dao)

        assert(repository.exercises().first() == catalogExercises)
        assert(dao.catalogCommit() == repdbCatalogCommit)
        assert(dao.observeAll().first().all { it.source == ExerciseSource.SHIPPED })
        assert(dao.observeAll().first().all { !it.isArchived })
    }

    @Test
    fun importDoesNotClobberExistingIds() = runBlocking {
        val dao = db.exerciseDao()
        val repository = WorkoutsRepositoryImpl(dao)
        val bench = catalogExercises.first { it.id == "bench-press" }
        repository.exercises().first()

        val crunch = ExerciseDefinition(
            "crunch-machine",
            "Crunch (Machine)",
            BodyPart.Core,
            Equipment.Machine,
            Mechanic.Isolation,
        )
        val imported = repository.importHistory(
            plans = emptyList(),
            sessions = emptyList(),
            exercises = listOf(crunch, bench.copy(name = "Other")),
        )

        assert(imported.exercises == listOf(crunch))
        val stored = repository.exercises().first()
        assert(stored.last() == crunch)
        assert(stored.single { it.id == bench.id } == bench)
        val entities = dao.observeAll().first()
        assert(entities.single { it.id == crunch.id }.source == ExerciseSource.IMPORT)
        assert(entities.single { it.id == bench.id }.source == ExerciseSource.SHIPPED)

        val refreshed = WorkoutsRepositoryImpl(
            dao,
            catalog = catalogExercises.map { exercise ->
                if (exercise.id == bench.id) exercise.copy(name = "Updated Bench") else exercise
            } + crunch.copy(name = "Shipped Crunch"),
            repdbCommit = "next-commit",
        )
        val afterRefresh = refreshed.exercises().first()
        assert(afterRefresh.single { it.id == bench.id }.name == "Updated Bench")
        assert(afterRefresh.single { it.id == crunch.id } == crunch)
        assert(dao.catalogCommit() == "next-commit")
    }
}
