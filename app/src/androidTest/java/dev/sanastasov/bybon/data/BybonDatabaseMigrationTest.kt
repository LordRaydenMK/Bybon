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

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
