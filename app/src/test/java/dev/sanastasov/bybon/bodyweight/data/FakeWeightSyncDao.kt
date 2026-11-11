package dev.sanastasov.bybon.bodyweight.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeWeightSyncDao : WeightSyncDao {
    private val ops = MutableStateFlow<List<WeightSyncOpEntity>>(emptyList())
    private val states = MutableStateFlow<Map<String, WeightSyncStateEntity>>(emptyMap())
    private val clashes = MutableStateFlow<List<WeightSyncClashEntity>>(emptyList())

    override fun observeOps(): Flow<List<WeightSyncOpEntity>> = ops

    override suspend fun ops(): List<WeightSyncOpEntity> = ops.value

    override suspend fun upsertOp(op: WeightSyncOpEntity) {
        ops.update { current -> current.filterNot { it.date == op.date } + op }
    }

    override suspend fun clearOps() {
        ops.value = emptyList()
    }

    override suspend fun deleteOps(dates: List<LocalDate>) {
        ops.update { current -> current.filterNot { it.date in dates } }
    }

    override fun observeState(databaseName: String): Flow<WeightSyncStateEntity?> =
        states.map { it[databaseName] }

    override suspend fun state(databaseName: String): WeightSyncStateEntity? =
        states.value[databaseName]

    override suspend fun upsertState(state: WeightSyncStateEntity) {
        states.update { it + (state.databaseName to state) }
    }

    override fun observeClashes(databaseName: String): Flow<List<WeightSyncClashEntity>> =
        clashes.map { rows -> rows.filter { it.databaseName == databaseName } }

    override suspend fun clashes(databaseName: String): List<WeightSyncClashEntity> =
        clashes.value.filter { it.databaseName == databaseName }

    override suspend fun deleteClashes(databaseName: String) {
        clashes.update { rows -> rows.filterNot { it.databaseName == databaseName } }
    }

    override suspend fun insertClashes(clashes: List<WeightSyncClashEntity>) {
        this.clashes.update { current -> current + clashes }
    }
}
