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
        val dbModule = DbModule.create(application)
        val module = MainModule.create(
            BodyWeightModule.create(dbModule),
            WorkoutModule.create(dbModule),
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
