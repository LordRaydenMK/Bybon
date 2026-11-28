package dev.sanastasov.bybon.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration7To8 : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `workout_plan` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0",
        )
        connection.execSQL(
            """
            UPDATE `workout_plan`
            SET `position` = (
                SELECT COUNT(*) FROM `workout_plan` AS older
                WHERE older.rowid < `workout_plan`.rowid
            )
            """.trimIndent(),
        )
    }
}
