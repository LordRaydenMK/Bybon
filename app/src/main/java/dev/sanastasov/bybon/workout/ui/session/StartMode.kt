package dev.sanastasov.bybon.workout.ui.session

import kotlinx.serialization.Serializable

@Serializable
enum class StartMode {
    Create,
    Resume,
    StartDraft,
}
