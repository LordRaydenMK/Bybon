package dev.sanastasov.bybon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.sanastasov.bybon.ui.theme.BybonTheme
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId

class MainActivity : ComponentActivity() {
    private var openSessionNonce = 0
    private var openSessionRequest by mutableStateOf<OpenSessionRequest>(OpenSessionRequest.None)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openSessionRequest = OpenSessionRequest.from(intent, openSessionNonce)
        val module = mainModule
        setContent {
            BybonTheme {
                with(module) {
                    BybonApp(openSessionRequest)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openSessionNonce++
        openSessionRequest = OpenSessionRequest.from(intent, openSessionNonce)
    }

    companion object {
        const val EXTRA_PLAN_ID = "dev.sanastasov.bybon.extra.PLAN_ID"

        fun create(context: Context, planId: WorkoutPlanId): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_PLAN_ID, planId.id)
            }
    }
}
