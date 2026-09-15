package dev.sanastasov.bybon.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import dev.sanastasov.bybon.bodyweight.data.DietPhaseEntity
import java.nio.file.Files
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BybonDatabaseMigrationTest {

    @Test
    fun migrateFrom1To2PreservesWeightEntriesAndAddsDietPhaseTable() = runTest {
        val databasePath = Files.createTempFile("bybon-migration", ".db")
        BundledSQLiteDriver().open(databasePath.toString()).use { connection ->
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS `weight_entry` " +
                    "(`date` TEXT NOT NULL, `weight` INTEGER NOT NULL, PRIMARY KEY(`date`))",
            )
            connection.execSQL(
                "INSERT INTO weight_entry (date, weight) VALUES ('2026-09-09', 6500)",
            )
            connection.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)",
            )
            connection.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                    "VALUES (42, 'bbc4abb1e3bfc4307b4505c150f16ad2')",
            )
            connection.execSQL("PRAGMA user_version = 1")
        }

        val db = Room.databaseBuilder(databasePath.toString()) { BybonDatabase_Impl() }
            .setDriver(BundledSQLiteDriver())
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
}
