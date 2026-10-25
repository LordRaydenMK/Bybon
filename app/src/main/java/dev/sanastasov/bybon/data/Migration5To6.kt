package dev.sanastasov.bybon.data

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration5To6 : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        // Partial unique index is created in OpenSessionIndexCallback and is not part of the
        // Room schema. Drop it so validation of workout_session succeeds after this upgrade.
        connection.execSQL("DROP INDEX IF EXISTS `idx_one_open_session_per_plan`")
        connection.execSQL("ALTER TABLE `workout_set` ADD COLUMN `completed_at` TEXT")
    }
}
