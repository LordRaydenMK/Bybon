package dev.sanastasov.bybon.workout.data

internal fun <T, I> List<T>.withoutExisting(existing: List<T>, id: (T) -> I): List<T> {
    val existingIds = existing.map(id).toSet()
    return fold(emptyList<T>() to existingIds) { (kept, seen), item ->
        val itemId = id(item)
        if (itemId in seen) {
            kept to seen
        } else {
            (kept + item) to (seen + itemId)
        }
    }.first
}
