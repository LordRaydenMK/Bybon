package dev.sanastasov.bybon.data

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

private const val DROP_ONE_OPEN_SESSION_INDEX =
    "DROP INDEX IF EXISTS `idx_one_open_session_per_plan`"

private const val CREATE_ONE_OPEN_SESSION_INSERT_TRIGGER =
    """
    CREATE TRIGGER IF NOT EXISTS `trg_one_open_session_per_plan_insert`
    BEFORE INSERT ON `workout_session`
    WHEN NEW.`duration_seconds` IS NULL
    BEGIN
        SELECT RAISE(ABORT, 'only one open session per plan')
        WHERE EXISTS (
            SELECT 1 FROM `workout_session`
            WHERE `plan_id` = NEW.`plan_id` AND `duration_seconds` IS NULL
        );
    END
    """

private const val CREATE_ONE_OPEN_SESSION_UPDATE_TRIGGER =
    """
    CREATE TRIGGER IF NOT EXISTS `trg_one_open_session_per_plan_update`
    BEFORE UPDATE ON `workout_session`
    WHEN NEW.`duration_seconds` IS NULL
    BEGIN
        SELECT RAISE(ABORT, 'only one open session per plan')
        WHERE EXISTS (
            SELECT 1 FROM `workout_session`
            WHERE `plan_id` = NEW.`plan_id` AND `duration_seconds` IS NULL AND `id` != NEW.`id`
        );
    END
    """

class OpenSessionIndexCallback : RoomDatabase.Callback() {
    override suspend fun onOpen(connection: SQLiteConnection) {
        connection.execSQL(DROP_ONE_OPEN_SESSION_INDEX)
        connection.execSQL(CREATE_ONE_OPEN_SESSION_INSERT_TRIGGER)
        connection.execSQL(CREATE_ONE_OPEN_SESSION_UPDATE_TRIGGER)
    }
}
