package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "weight_sync_op")
data class WeightSyncOpEntity(
    @PrimaryKey val date: LocalDate,
    val kind: String,
    val weight: Int?,
) {
    companion object {
        const val UPSERT = "upsert"
        const val DELETE = "delete"
    }
}
