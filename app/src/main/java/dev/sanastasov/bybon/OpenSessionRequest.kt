package dev.sanastasov.bybon

import android.content.Intent
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId

sealed class OpenSessionRequest {
    data object None : OpenSessionRequest()
    data class Open(
        val planId: WorkoutPlanId,
        val nonce: Int,
    ) : OpenSessionRequest()

    companion object {
        fun from(intent: Intent, nonce: Int): OpenSessionRequest {
            val planId = intent.getStringExtra(MainActivity.EXTRA_PLAN_ID)
                ?.takeUnless(String::isBlank)
                ?.let(::WorkoutPlanId)
                ?: return None
            return Open(planId, nonce)
        }
    }
}
