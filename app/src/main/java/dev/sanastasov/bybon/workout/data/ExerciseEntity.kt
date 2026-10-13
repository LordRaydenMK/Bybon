package dev.sanastasov.bybon.workout.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "exercise",
    indices = [
        Index(value = ["source"], name = "index_exercise_source"),
        Index(value = ["is_archived"], name = "index_exercise_is_archived"),
        Index(value = ["body_part"], name = "index_exercise_body_part"),
        Index(value = ["name"], name = "index_exercise_name"),
    ],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "body_part") val bodyPart: String?,
    val equipment: String?,
    val mechanic: String?,
    @ColumnInfo(name = "primary_muscles", defaultValue = "'[]'") val primaryMuscles: String,
    @ColumnInfo(name = "secondary_muscles", defaultValue = "'[]'") val secondaryMuscles: String,
    @ColumnInfo(name = "equipment_slug") val equipmentSlug: String?,
    @ColumnInfo(name = "force_type") val forceType: String?,
    val difficulty: String?,
    @ColumnInfo(defaultValue = "'[]'") val tags: String,
    val description: String?,
    val instructions: String?,
    val tips: String?,
    @ColumnInfo(name = "image_start") val imageStart: String?,
    @ColumnInfo(name = "image_peak") val imagePeak: String?,
    @ColumnInfo(name = "image_main") val imageMain: String?,
    val source: String,
    @ColumnInfo(name = "is_archived", defaultValue = "0") val isArchived: Boolean = false,
)

internal object ExerciseSource {
    const val SHIPPED = "shipped"
    const val IMPORT = "import"
    const val USER = "user"
}
