package dev.sanastasov.bybon.bodyweight.sync

interface WeightCloud {
    suspend fun readAll(uid: String): List<RemoteWeight>

    suspend fun readUpdatedAfter(uid: String, cursorMillis: Long): List<RemoteWeight>

    suspend fun write(uid: String, documents: List<WeightUpload>)
}
