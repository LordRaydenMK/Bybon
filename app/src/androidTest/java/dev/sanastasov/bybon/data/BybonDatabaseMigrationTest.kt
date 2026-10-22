package dev.sanastasov.bybon.data

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.sanastasov.bybon.bodyweight.data.DietPhaseEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BybonDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = instrumentation.targetContext.getDatabasePath(TEST_DB),
        driver = AndroidSQLiteDriver(),
        databaseClass = BybonDatabase::class,
    )

    @Before
    fun deleteDatabase() {
        instrumentation.targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun migrateFrom1To2PreservesWeightEntriesAndAddsDietPhaseTable() = runBlocking {
        helper.createDatabase(1).use { connection ->
            connection.execSQL(
                "INSERT INTO weight_entry (date, weight) VALUES ('2026-09-09', 6500)",
            )
        }
        helper.runMigrationsAndValidate(2).close()

        val db = Room.databaseBuilder<BybonDatabase>(instrumentation.targetContext, TEST_DB)
            .setDriver(AndroidSQLiteDriver())
            .build()
        try {
            val weights = db.weightEntryDao().weightEntries().first()
            assert(weights.single().date == LocalDate.of(2026, 9, 9))
            assert(weights.single().weight == 6500)

            val dietPhaseDao = db.dietPhaseDao()
            assert(dietPhaseDao.phases().first().isEmpty())

            dietPhaseDao.upsert(
                DietPhaseEntity(
                    kind = "Maintain",
                    startDate = LocalDate.of(2026, 9, 7),
                    startWeight = 6500,
                    targetWeight = 6500,
                    durationWeeks = null,
                    endedAt = null,
                ),
            )
            val phases = dietPhaseDao.phases().first()
            assert(phases.single().kind == "Maintain")
            assert(phases.single().startWeight == 6500)
            assert(phases.single().endedAt == null)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrateFrom2To3CreatesExerciseTableAndKeepsWeightEntries() = runBlocking {
        helper.createDatabase(2).use { connection ->
            connection.execSQL(
                "INSERT INTO weight_entry (date, weight) VALUES ('2026-09-09', 6500)",
            )
            connection.execSQL(
                """
                INSERT INTO diet_phase (kind, startDate, startWeight, targetWeight, durationWeeks, endedAt)
                VALUES ('Maintain', '2026-09-07', 6500, 6500, NULL, NULL)
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(3).close()

        val db = Room.databaseBuilder<BybonDatabase>(instrumentation.targetContext, TEST_DB)
            .setDriver(AndroidSQLiteDriver())
            .build()
        try {
            val weights = db.weightEntryDao().weightEntries().first()
            assert(weights.single().date == LocalDate.of(2026, 9, 9))
            assert(weights.single().weight == 6500)
            val phases = db.dietPhaseDao().phases().first()
            assert(phases.single().kind == "Maintain")
            assert(db.exerciseDao().observeAll().first().isEmpty())
            assert(db.exerciseDao().catalogCommit() == null)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrateFrom3To4CreatesPlanTablesAndKeepsExerciseCatalog() = runBlocking {
        helper.createDatabase(3).use { connection ->
            connection.execSQL(
                "INSERT INTO weight_entry (date, weight) VALUES ('2026-09-09', 6500)",
            )
            connection.execSQL(
                """
                INSERT INTO diet_phase (kind, startDate, startWeight, targetWeight, durationWeeks, endedAt)
                VALUES ('Maintain', '2026-09-07', 6500, 6500, NULL, NULL)
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO exercise (
                    id, name, body_part, equipment, mechanic, primary_muscles, secondary_muscles,
                    equipment_slug, force_type, difficulty, tags, description, instructions, tips,
                    image_start, image_peak, image_main, source, is_archived
                ) VALUES (
                    'bench-press', 'Barbell Bench Press', 'Chest', 'Barbell', 'Compound', '[]', '[]',
                    NULL, NULL, NULL, '[]', NULL, NULL, NULL,
                    NULL, NULL, NULL, 'shipped', 0
                )
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(4).close()

        val db = Room.databaseBuilder<BybonDatabase>(instrumentation.targetContext, TEST_DB)
            .setDriver(AndroidSQLiteDriver())
            .build()
        try {
            val weights = db.weightEntryDao().weightEntries().first()
            assert(weights.single().date == LocalDate.of(2026, 9, 9))
            assert(weights.single().weight == 6500)
            val phases = db.dietPhaseDao().phases().first()
            assert(phases.single().kind == "Maintain")
            assert(db.exerciseDao().observeAll().first().single().id == "bench-press")
            assert(db.workoutPlanDao().observePlans().first().isEmpty())
            assert(db.workoutPlanDao().observePlanExercises().first().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun migrateFrom4To5CreatesSessionTablesAndKeepsPlans() = runBlocking {
        helper.createDatabase(4).use { connection ->
            connection.execSQL(
                "INSERT INTO weight_entry (date, weight) VALUES ('2026-09-09', 6500)",
            )
            connection.execSQL(
                """
                INSERT INTO diet_phase (kind, startDate, startWeight, targetWeight, durationWeeks, endedAt)
                VALUES ('Maintain', '2026-09-07', 6500, 6500, NULL, NULL)
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO exercise (
                    id, name, body_part, equipment, mechanic, primary_muscles, secondary_muscles,
                    equipment_slug, force_type, difficulty, tags, description, instructions, tips,
                    image_start, image_peak, image_main, source, is_archived
                ) VALUES (
                    'bench-press', 'Barbell Bench Press', 'Chest', 'Barbell', 'Compound', '[]', '[]',
                    NULL, NULL, NULL, '[]', NULL, NULL, NULL,
                    NULL, NULL, NULL, 'shipped', 0
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO workout_plan (id, name, description, is_archived)
                VALUES ('full-body-a', 'Full Body A', 'Full body workout routine variant A', 0)
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO workout_plan_exercise (
                    plan_id, exercise_id, position, warmup_sets, work_sets, rep_min, rep_max,
                    rest_after_work_set_seconds, notes
                ) VALUES ('full-body-a', 'bench-press', 0, 3, 3, 8, 10, 120, '[]')
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(5).close()

        val db = Room.databaseBuilder<BybonDatabase>(instrumentation.targetContext, TEST_DB)
            .setDriver(AndroidSQLiteDriver())
            .addCallback(OpenSessionIndexCallback())
            .build()
        try {
            assert(db.weightEntryDao().weightEntries().first().single().weight == 6500)
            assert(db.workoutPlanDao().observePlans().first().single().id == "full-body-a")
            assert(db.workoutSessionDao().observeSessions().first().isEmpty())
            assert(db.workoutSessionDao().observeSessionExercises().first().isEmpty())
            assert(db.workoutSessionDao().observeSets().first().isEmpty())
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
