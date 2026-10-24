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
import dev.sanastasov.bybon.data.converter.LocalDateTimeConverter
import dev.sanastasov.bybon.workout.data.ExerciseDao
import dev.sanastasov.bybon.workout.data.ExerciseEntity
import dev.sanastasov.bybon.workout.data.RepdbCatalogEntity
import dev.sanastasov.bybon.workout.data.WorkoutPlanDao
import dev.sanastasov.bybon.workout.data.WorkoutPlanEntity
import dev.sanastasov.bybon.workout.data.WorkoutPlanExerciseEntity
import dev.sanastasov.bybon.workout.data.WorkoutSessionDao
import dev.sanastasov.bybon.workout.data.WorkoutSessionEntity
import dev.sanastasov.bybon.workout.data.WorkoutSessionExerciseEntity
import dev.sanastasov.bybon.workout.data.WorkoutSetEntity

@Database(
    entities = [
        WeightEntryEntity::class,
        DietPhaseEntity::class,
        ExerciseEntity::class,
        RepdbCatalogEntity::class,
        WorkoutPlanEntity::class,
        WorkoutPlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSessionExerciseEntity::class,
        WorkoutSetEntity::class,
    ],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ],
)
@ColumnTypeConverters(LocalDateConverter::class, LocalDateTimeConverter::class)
abstract class BybonDatabase : RoomDatabase() {
    abstract fun weightEntryDao(): WeightDao
    abstract fun dietPhaseDao(): DietPhaseDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
}
