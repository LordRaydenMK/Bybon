package dev.sanastasov.bybon.data

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import dev.sanastasov.bybon.bodyweight.data.DietPhaseDao
import dev.sanastasov.bybon.bodyweight.data.DietPhaseEntity
import dev.sanastasov.bybon.bodyweight.data.WeightDao
import dev.sanastasov.bybon.bodyweight.data.WeightEntryEntity
import dev.sanastasov.bybon.data.converter.LocalDateConverter
import dev.sanastasov.bybon.workout.data.ExerciseDao
import dev.sanastasov.bybon.workout.data.ExerciseEntity
import dev.sanastasov.bybon.workout.data.RepdbCatalogEntity
import dev.sanastasov.bybon.workout.data.WorkoutPlanDao
import dev.sanastasov.bybon.workout.data.WorkoutPlanEntity
import dev.sanastasov.bybon.workout.data.WorkoutPlanExerciseEntity

@Database(
    entities = [
        WeightEntryEntity::class,
        DietPhaseEntity::class,
        ExerciseEntity::class,
        RepdbCatalogEntity::class,
        WorkoutPlanEntity::class,
        WorkoutPlanExerciseEntity::class,
    ],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
)
@ColumnTypeConverters(LocalDateConverter::class)
abstract class BybonDatabase : RoomDatabase() {
    abstract fun weightEntryDao(): WeightDao
    abstract fun dietPhaseDao(): DietPhaseDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
}
