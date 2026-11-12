package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "weight_sync_clash")
data class WeightSyncClashEntity(
    @PrimaryKey val date: LocalDate,
    val phoneWeight: Int,
    val backupWeight: Int?,
    val backupDeleted: Boolean,
)
