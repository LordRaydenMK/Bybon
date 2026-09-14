package dev.sanastasov.bybon.data

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Rule
import org.junit.Test

class BybonDatabaseMigrationTest {

    private val databasePath: Path = Files.createTempFile("bybon-migration", ".db")

    @get:Rule
    val helper = MigrationTestHelper(
        schemaDirectoryPath = schemaDirectory(),
        databasePath = databasePath,
        driver = BundledSQLiteDriver(),
        databaseClass = BybonDatabase::class,
    )

    @Test
    fun migrateFrom1To2PreservesWeightEntriesAndAddsDietPhaseTable() {
        helper.createDatabase(1).use { connection ->
            connection.execSQL(
                "INSERT INTO weight_entry (date, weight) VALUES ('2026-09-09', 6500)",
            )
        }

        helper.runMigrationsAndValidate(2).use { connection ->
            connection.prepare("SELECT date, weight FROM weight_entry").use { statement ->
                assert(statement.step())
                assert(statement.getText(0) == "2026-09-09")
                assert(statement.getLong(1) == 6500L)
                assert(!statement.step())
            }
            connection.prepare(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'diet_phase'",
            ).use { statement ->
                assert(statement.step())
            }
            connection.prepare("SELECT COUNT(*) FROM diet_phase").use { statement ->
                assert(statement.step())
                assert(statement.getLong(0) == 0L)
            }
        }
    }
}

private fun schemaDirectory(): Path {
    val candidates = listOf(
        Path.of("schemas"),
        Path.of("app/schemas"),
    )
    return candidates.firstOrNull { path ->
        Files.isDirectory(path.resolve("dev.sanastasov.bybon.data.BybonDatabase"))
    } ?: error("Room schema directory not found from ${Path.of("").toAbsolutePath()}")
}
