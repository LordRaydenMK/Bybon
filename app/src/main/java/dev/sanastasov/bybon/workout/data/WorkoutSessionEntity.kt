package dev.sanastasov.bybon.workout.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "workout_session",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(
            value = ["plan_id", "started_at"],
            name = "index_workout_session_plan_id_started_at",
            unique = true,
        ),
        Index(
            value = ["started_at"],
            name = "index_workout_session_started_at",
        ),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "plan_name") val planName: String,
    val note: String?,
    @ColumnInfo(name = "started_at") val startedAt: LocalDateTime,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int?,
)

@Entity(
    tableName = "workout_session_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exercise_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(
            value = ["session_id", "position"],
            name = "index_workout_session_exercise_session_id_position",
            unique = true,
        ),
        Index(
            value = ["session_id", "exercise_id"],
            name = "index_workout_session_exercise_session_id_exercise_id",
            unique = true,
        ),
    ],
)
data class WorkoutSessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    val position: Int,
    @ColumnInfo(name = "rep_min") val repMin: Int,
    @ColumnInfo(name = "rep_max") val repMax: Int,
    @ColumnInfo(name = "rest_after_work_set_seconds") val restAfterWorkSetSeconds: Int,
    @ColumnInfo(defaultValue = "'[]'") val notes: String = "[]",
)

@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_exercise_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["session_exercise_id", "position"],
            name = "index_workout_set_session_exercise_id_position",
            unique = true,
        ),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_exercise_id") val sessionExerciseId: Long,
    val position: Int,
    @ColumnInfo(name = "is_warmup") val isWarmup: Boolean,
    val weight: Int?,
    val reps: Int,
    @ColumnInfo(name = "set_state") val setState: String,
    @ColumnInfo(name = "previous_weight") val previousWeight: Int?,
    @ColumnInfo(name = "previous_reps") val previousReps: Int?,
    @ColumnInfo(name = "completed_at") val completedAt: LocalDateTime? = null,
)

data class WorkoutSessionIdentity(
    @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "started_at") val startedAt: LocalDateTime,
)
