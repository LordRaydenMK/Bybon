package dev.sanastasov.bybon

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import dev.sanastasov.bybon.ui.theme.BybonTheme
import dev.sanastasov.bybon.workout.workoutSessionPlanId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val module = mainModule
        setContent {
            var openSessionPlanId by remember { mutableStateOf(intent.workoutSessionPlanId()) }
            var openSessionNonce by remember { mutableIntStateOf(0) }
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    setIntent(newIntent)
                    openSessionPlanId = newIntent.workoutSessionPlanId()
                    openSessionNonce++
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }
            BybonTheme {
                with(module) {
                    BybonApp(openSessionPlanId, openSessionNonce)
                }
            }
        }
    }
}
