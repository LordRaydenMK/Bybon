package dev.sanastasov.bybon.bodyweight.data

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dev.sanastasov.bybon.BuildConfig
import dev.sanastasov.bybon.bodyweight.sync.RemoteWeight
import dev.sanastasov.bybon.bodyweight.sync.WeightCloud
import dev.sanastasov.bybon.bodyweight.sync.WeightCloudException
import dev.sanastasov.bybon.bodyweight.sync.WeightUpload
import dev.sanastasov.bybon.bodyweight.sync.parseWeightDocument
import java.io.IOException
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirestoreWeightCloud(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(
        FirebaseApp.getInstance(),
        BuildConfig.FIREBASE_ENV,
    ),
) : WeightCloud {

    override suspend fun readAll(uid: String): List<RemoteWeight> =
        weights(uid).get().awaitCloud().documents.mapNotNull { it.toRemote() }

    override suspend fun readUpdatedAfter(uid: String, cursorMillis: Long): List<RemoteWeight> =
        weights(uid)
            .whereGreaterThan("updatedAt", Timestamp(Date(cursorMillis)))
            .get()
            .awaitCloud()
            .documents
            .mapNotNull { it.toRemote() }

    override suspend fun write(uid: String, documents: List<WeightUpload>) {
        if (documents.isEmpty()) return
        documents.chunked(WRITE_CHUNK).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { upload ->
                val data = mutableMapOf<String, Any>(
                    "deleted" to upload.deleted,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                upload.weight?.let { data["weight"] = it }
                batch.set(weights(uid).document(upload.date.toString()), data)
            }
            batch.commit().awaitCloud()
        }
    }

    private fun weights(uid: String) =
        firestore.collection("users").document(uid).collection("weights")

    private companion object {
        const val WRITE_CHUNK = 400
    }
}

private suspend fun <T> Task<T>.awaitCloud(): T = try {
    await()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: FirebaseException) {
    throw WeightCloudException(failure)
} catch (failure: IOException) {
    throw WeightCloudException(failure)
}

private fun DocumentSnapshot.toRemote(): RemoteWeight? = parseWeightDocument(
    id = id,
    weight = getLong("weight"),
    deleted = getBoolean("deleted") ?: false,
    updatedAtMillis = getTimestamp("updatedAt")?.toDate()?.time,
)
