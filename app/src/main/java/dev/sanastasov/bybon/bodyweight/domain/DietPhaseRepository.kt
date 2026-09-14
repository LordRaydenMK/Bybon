package dev.sanastasov.bybon.bodyweight.domain

import kotlinx.coroutines.flow.Flow

interface DietPhaseRepository {

    fun openPhase(): Flow<DietPhaseRecord?>

    suspend fun apply(phase: DietPhase)

    suspend fun clear()
}
