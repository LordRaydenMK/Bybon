package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "weight_sync_state")
data class WeightSyncStateEntity(
    @PrimaryKey val databaseName: String,
    val lastSyncedUid: String?,
    val cursorMillis: Long?,
    val lastSuccessMillis: Long?,
    val conflictPending: Boolean,
    val summaryCount: Int?,
    val summaryEarliest: LocalDate?,
    val summaryLatest: LocalDate?,
)
