package dev.sanastasov.bybon.workout.data

internal fun <T, I> List<T>.withoutExisting(existing: List<T>, id: (T) -> I): List<T> =
    withoutExistingIds(existing.map(id).toSet(), id)

internal fun <T, I> List<T>.withoutExistingIds(existingIds: Set<I>, id: (T) -> I): List<T> =
    fold(emptyList<T>() to existingIds) { (kept, seen), item ->
        val itemId = id(item)
        if (itemId in seen) {
            kept to seen
        } else {
            (kept + item) to (seen + itemId)
        }
    }.first
