package dev.sanastasov.bybon.bodyweight.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeWeightSyncDao : WeightSyncDao {
    private val ops = MutableStateFlow<List<WeightSyncOpEntity>>(emptyList())
    private val state = MutableStateFlow<WeightSyncStateEntity?>(null)
    private val clashes = MutableStateFlow<List<WeightSyncClashEntity>>(emptyList())

    override fun observeOps(): Flow<List<WeightSyncOpEntity>> = ops

    override suspend fun upsertOp(op: WeightSyncOpEntity) {
        ops.update { current -> current.filterNot { it.date == op.date } + op }
    }

    override suspend fun clearOps() {
        ops.value = emptyList()
    }

    override suspend fun deleteOps(dates: List<LocalDate>) {
        ops.update { current -> current.filterNot { it.date in dates } }
    }

    override fun observeState(): Flow<WeightSyncStateEntity?> = state

    override suspend fun upsertState(state: WeightSyncStateEntity) {
        this.state.value = state
    }

    override fun observeClashes(): Flow<List<WeightSyncClashEntity>> = clashes

    override suspend fun deleteClashes() {
        clashes.value = emptyList()
    }

    override suspend fun insertClashes(clashes: List<WeightSyncClashEntity>) {
        this.clashes.update { current -> current + clashes }
    }
}
