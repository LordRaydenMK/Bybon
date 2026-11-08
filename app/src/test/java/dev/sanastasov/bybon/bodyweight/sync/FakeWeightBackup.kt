package dev.sanastasov.bybon.bodyweight.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeWeightBackup : WeightBackup {

    val status = MutableStateFlow(WeightBackupStatus())
    val syncs = mutableListOf<Pair<String, Boolean>>()
    var kept = false
        private set
    var used = false
        private set
    var replaced = false
        private set

    override fun observe(databaseName: String): Flow<WeightBackupStatus> = status

    override suspend fun sync(databaseName: String, uid: String, signIn: Boolean) {
        syncs += uid to signIn
    }

    override suspend fun keepThisPhone(databaseName: String) {
        kept = true
        status.update { it.copy(conflict = false) }
    }

    override suspend fun useBackup(databaseName: String, uid: String) {
        used = true
    }

    override suspend fun replaceBackup(databaseName: String, uid: String) {
        replaced = true
    }

    override suspend fun dismissSummary(databaseName: String) {
        status.update { it.copy(summary = null) }
    }
}
