package dev.sanastasov.bybon.data

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import dev.sanastasov.bybon.data.converter.LocalDateConverter
import dev.sanastasov.bybon.weight.data.WeightDao
import dev.sanastasov.bybon.weight.data.WeightEntryEntity

@Database(entities = [WeightEntryEntity::class], version = 1)
@ColumnTypeConverters(LocalDateConverter::class)
abstract class BybonDatabase : RoomDatabase() {
    abstract fun weightEntryDao(): WeightDao
}
