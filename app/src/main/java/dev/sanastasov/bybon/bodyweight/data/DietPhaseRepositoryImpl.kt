package dev.sanastasov.bybon.bodyweight.data

import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.domain.DietPhase
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseKind
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRecord
import dev.sanastasov.bybon.bodyweight.domain.DietPhaseRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DietPhaseRepositoryImpl(
    private val dao: DietPhaseDao,
    private val today: () -> LocalDate = { LocalDate.now() },
) : DietPhaseRepository {

    override fun openPhase(): Flow<DietPhaseRecord?> = dao.openPhase().map { entity ->
        entity?.toRecord()
    }

    override suspend fun apply(phase: DietPhase) {
        dao.endOpenPhases(today())
        dao.insert(phase.toEntity())
    }

    override suspend fun clear() {
        dao.endOpenPhases(today())
    }
}

private fun DietPhase.toEntity(): DietPhaseEntity = DietPhaseEntity(
    kind = kind.name,
    startDate = startDate,
    startWeight = startWeight.value,
    targetWeight = targetWeight.value,
    durationWeeks = durationWeeks,
    endedAt = null,
)

private fun DietPhaseEntity.toRecord(): DietPhaseRecord = DietPhaseRecord(
    id = id,
    phase = DietPhase(
        kind = DietPhaseKind.valueOf(kind),
        startDate = startDate,
        startWeight = BodyWeight(startWeight),
        targetWeight = BodyWeight(targetWeight),
        durationWeeks = durationWeeks,
    ),
    endedAt = endedAt,
)
