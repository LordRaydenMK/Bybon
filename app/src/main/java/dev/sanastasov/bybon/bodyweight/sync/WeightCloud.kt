package dev.sanastasov.bybon.bodyweight.sync

import java.time.Instant

interface WeightCloud {
    suspend fun readAll(uid: String): List<RemoteWeight>

    suspend fun readUpdatedAfter(uid: String, cursor: Instant): List<RemoteWeight>

    suspend fun write(uid: String, documents: List<WeightUpload>)
}
