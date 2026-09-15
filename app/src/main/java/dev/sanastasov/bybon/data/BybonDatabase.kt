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

@Database(
    entities = [WeightEntryEntity::class, DietPhaseEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ColumnTypeConverters(LocalDateConverter::class)
abstract class BybonDatabase : RoomDatabase() {
    abstract fun weightEntryDao(): WeightDao
    abstract fun dietPhaseDao(): DietPhaseDao
}
