package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.LocalDate

@Entity("weight_entry")
data class WeightEntryEntity(
    @PrimaryKey val date: LocalDate,
    @ColumnInfo("weight") val weight: Int,
)