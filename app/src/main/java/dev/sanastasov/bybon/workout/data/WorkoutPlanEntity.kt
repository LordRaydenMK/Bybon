package dev.sanastasov.bybon.workout.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "workout_plan",
    indices = [
        Index(value = ["is_archived"], name = "index_workout_plan_is_archived"),
    ],
)
data class WorkoutPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Boolean = false,
)

@Entity(
    tableName = "workout_plan_exercise",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
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
            value = ["plan_id", "position"],
            name = "index_workout_plan_exercise_plan_id_position",
            unique = true,
        ),
        Index(
            value = ["plan_id", "exercise_id"],
            name = "index_workout_plan_exercise_plan_id_exercise_id",
            unique = true,
        ),
    ],
)
data class WorkoutPlanExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    val position: Int,
    @ColumnInfo(name = "warmup_sets", defaultValue = "0") val warmupSets: Int = 0,
    @ColumnInfo(name = "work_sets") val workSets: Int,
    @ColumnInfo(name = "rep_min") val repMin: Int,
    @ColumnInfo(name = "rep_max") val repMax: Int,
    @ColumnInfo(name = "rest_after_work_set_seconds") val restAfterWorkSetSeconds: Int,
    @ColumnInfo(defaultValue = "'[]'") val notes: String = "[]",
)
