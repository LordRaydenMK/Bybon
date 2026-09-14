package dev.sanastasov.bybon.data

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Runs the generated Room AutoMigration from v1 to v2 on bundled SQLite.
 *
 * Unit tests here cannot use MigrationTestHelper: the Android artifact needs Instrumentation,
 * and the JVM artifact does not match this module's Android Room runtime.
 */
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

            BybonDatabase_AutoMigration_1_2_Impl().migrate(connection)

            connection.prepare("SELECT date, weight FROM weight_entry").use { statement ->
                assert(statement.step())
                assert(statement.getText(0) == "2026-09-09")
                assert(statement.getLong(1) == 6500L)
                assert(!statement.step())
            }
            connection.prepare(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'diet_phase'",
            ).use { statement ->
                assert(statement.step())
                val sql = statement.getText(0)
                assert(sql.contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"))
                assert(sql.contains("`kind` TEXT NOT NULL"))
                assert(sql.contains("`startDate` TEXT NOT NULL"))
                assert(sql.contains("`startWeight` INTEGER NOT NULL"))
                assert(sql.contains("`targetWeight` INTEGER NOT NULL"))
                assert(sql.contains("`durationWeeks` INTEGER"))
                assert(sql.contains("`endedAt` TEXT"))
            }
            connection.prepare("SELECT COUNT(*) FROM diet_phase").use { statement ->
                assert(statement.step())
                assert(statement.getLong(0) == 0L)
            }
        }
    }
}
