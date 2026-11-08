package dev.sanastasov.bybon.bodyweight.domain

import java.time.LocalDate

data class WeightSyncOp(
    val date: LocalDate,
    val kind: WeightSyncOpKind,
    val weight: Int?,
)

enum class WeightSyncOpKind { Upsert, Delete }

data class WeightSyncState(
    val databaseName: String,
    val lastSyncedUid: String?,
    val cursorMillis: Long?,
    val lastSuccessMillis: Long?,
    val conflictPending: Boolean,
    val summaryCount: Int?,
    val summaryEarliest: LocalDate?,
    val summaryLatest: LocalDate?,
)
