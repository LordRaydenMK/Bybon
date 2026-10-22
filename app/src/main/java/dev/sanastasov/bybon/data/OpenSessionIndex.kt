package dev.sanastasov.bybon.data

import androidx.room3.RoomDatabase
import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal const val CREATE_ONE_OPEN_SESSION_INDEX =
    "CREATE UNIQUE INDEX IF NOT EXISTS `idx_one_open_session_per_plan` " +
        "ON `workout_session`(`plan_id`) WHERE `duration_seconds` IS NULL"

class From4To5 : AutoMigrationSpec {
    override suspend fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_ONE_OPEN_SESSION_INDEX)
    }
}

class OpenSessionIndexCallback : RoomDatabase.Callback() {
    override suspend fun onCreate(connection: SQLiteConnection) {
        connection.execSQL(CREATE_ONE_OPEN_SESSION_INDEX)
    }
}
