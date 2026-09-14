package dev.sanastasov.bybon.bodyweight

import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDietPhaseRepository(
    initial: DietPhaseRecord? = null,
) : DietPhaseRepository {

    private val records = MutableStateFlow(listOfNotNull(initial))
    private var nextId = (initial?.id ?: 0L) + 1

    override fun openPhase(): Flow<DietPhaseRecord?> = records.map { all ->
        all.lastOrNull { it.endedAt == null }
    }

    override suspend fun apply(phase: DietPhase) {
        clear()
        records.value = records.value + DietPhaseRecord(nextId++, phase)
    }

    override suspend fun clear() {
        records.value = records.value.map { record ->
            if (record.endedAt == null) record.copy(endedAt = LocalDate.now()) else record
        }
    }
}
