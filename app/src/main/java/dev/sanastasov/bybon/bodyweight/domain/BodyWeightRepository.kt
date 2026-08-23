package dev.sanastasov.bybon.bodyweight.domain

import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import kotlinx.coroutines.flow.Flow

interface BodyWeightRepository {

    fun entries(): Flow<List<BodyWeightEntry>>

    suspend fun insert(entry: BodyWeightEntry)
}
