package dev.sanastasov.bybon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.sanastasov.bybon.bodyweight.BodyWeightModule
import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.main.MainModule
import dev.sanastasov.bybon.ui.theme.BybonTheme
import dev.sanastasov.bybon.workout.WorkoutModule

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val module = MainModule.create(
            BodyWeightModule.create(DbModule.create(application)),
            WorkoutModule.create()
        )
        setContent {
            BybonTheme {
                with(module) {
                    BybonApp()
                }
            }
        }
    }
}
