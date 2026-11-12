package dev.sanastasov.bybon.bodyweight.sync

import java.time.LocalDate

interface WeightCloud {
    suspend fun readAll(uid: String): List<RemoteWeight>

    suspend fun readUpdatedAfter(uid: String, cursorMillis: Long): List<RemoteWeight>

    suspend fun write(uid: String, documents: List<WeightUpload>)
}

fun parseWeightDocument(
    id: String,
    weight: Long?,
    deleted: Boolean,
    updatedAtMillis: Long?,
): RemoteWeight? {
    val date = runCatching { LocalDate.parse(id) }.getOrNull()
    val liveWeight = weight?.toInt()
    val validLive = liveWeight != null &&
        liveWeight.toLong() == weight &&
        liveWeight.isSyncableWeight()
    return when {
        date == null || updatedAtMillis == null -> null

        deleted -> RemoteWeight(
            date = date,
            weight = weight?.toInt() ?: 0,
            deleted = true,
            updatedAtMillis = updatedAtMillis,
        )

        validLive -> RemoteWeight(
            date = date,
            weight = liveWeight,
            deleted = false,
            updatedAtMillis = updatedAtMillis,
        )

        else -> null
    }
}
