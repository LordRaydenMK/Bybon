package dev.sanastasov.bybon.bodyweight.sync

import kotlinx.coroutines.flow.Flow

data class WeightBackupStatus(
    val pending: Boolean = false,
    val lastSuccessMillis: Long? = null,
    val conflict: Boolean = false,
    val summary: WeightImportSummary? = null,
)

interface WeightBackup {
    fun observe(databaseName: String): Flow<WeightBackupStatus>

    suspend fun sync(databaseName: String, uid: String, signIn: Boolean)

    suspend fun keepThisPhone(databaseName: String)

    suspend fun useBackup(databaseName: String, uid: String)

    suspend fun replaceBackup(databaseName: String, uid: String)

    suspend fun dismissSummary(databaseName: String)
}
