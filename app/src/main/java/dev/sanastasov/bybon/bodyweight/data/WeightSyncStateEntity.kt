package dev.sanastasov.bybon.bodyweight.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "weight_sync_state")
data class WeightSyncStateEntity(
    @PrimaryKey val id: Int = 0,
    val lastSyncedUid: String?,
    val cursorMillis: Long?,
    val lastSuccessMillis: Long?,
    val conflictKind: String?,
    val conflictSignedInUid: String?,
    val conflictPreviousUid: String?,
    val summaryCount: Int?,
    val summaryEarliest: LocalDate?,
    val summaryLatest: LocalDate?,
) {
    companion object {
        const val ACCOUNT = "account"
        const val DATES = "dates"
    }
}
