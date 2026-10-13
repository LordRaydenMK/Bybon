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

@Database(
    entities = [WeightEntryEntity::class, DietPhaseEntity::class, ExerciseEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
)
@ColumnTypeConverters(LocalDateConverter::class)
abstract class BybonDatabase : RoomDatabase() {
    abstract fun weightEntryDao(): WeightDao
    abstract fun dietPhaseDao(): DietPhaseDao
    abstract fun exerciseDao(): ExerciseDao
}
