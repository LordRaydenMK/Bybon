package dev.sanastasov.bybon.bodyweight.sync

import dev.sanastasov.bybon.bodyweight.BodyWeight
import java.time.LocalDate

data class RemoteWeight(
    val date: LocalDate,
    val weight: BodyWeight?,
    val deleted: Boolean,
    val updatedAtMillis: Long,
) {
    init {
        require(deleted || weight != null) { "A live backup weight is missing" }
    }
}

fun parseWeightDocument(
    id: String,
    weight: Long?,
    deleted: Boolean,
    updatedAtMillis: Long?,
): RemoteWeight? {
    val date = runCatching { LocalDate.parse(id) }.getOrNull()
    if (date == null || updatedAtMillis == null) return null
    val parsed = weight?.toIntExactOrNull()?.let(BodyWeight::parseOrNull)
    return when {
        deleted -> RemoteWeight(
            date = date,
            weight = null,
            deleted = true,
            updatedAtMillis = updatedAtMillis,
        )

        parsed != null -> RemoteWeight(
            date = date,
            weight = parsed,
            deleted = false,
            updatedAtMillis = updatedAtMillis,
        )

        else -> null
    }
}

private fun Long.toIntExactOrNull(): Int? {
    val narrowed = toInt()
    return if (narrowed.toLong() == this) narrowed else null
}
