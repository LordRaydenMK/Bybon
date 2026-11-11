package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Entity
import androidx.room3.ForeignKey
import java.time.LocalDate

@Entity(
    tableName = "weight_sync_clash",
    primaryKeys = ["databaseName", "date"],
    foreignKeys = [
        ForeignKey(
            entity = WeightSyncStateEntity::class,
            parentColumns = ["databaseName"],
            childColumns = ["databaseName"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WeightSyncClashEntity(
    val databaseName: String,
    val date: LocalDate,
    val phoneWeight: Int,
    val backupWeight: Int?,
    val backupDeleted: Boolean,
)
