package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime

sealed interface SetState {
    data object NotStated : SetState
    data object InProgress : SetState
    data class Completed(
        val at: LocalDateTime? = null,
    ) : SetState

    companion object {
        fun fromStored(name: String, completedAt: LocalDateTime?): SetState = when (name) {
            "NotStated" -> NotStated
            "InProgress" -> InProgress
            "Completed" -> Completed(completedAt)
            else -> error("Unknown set state $name")
        }
    }
}

val SetState.storedName: String
    get() = when (this) {
        SetState.NotStated -> "NotStated"
        SetState.InProgress -> "InProgress"
        is SetState.Completed -> "Completed"
    }
