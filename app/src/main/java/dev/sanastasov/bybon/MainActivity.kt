package dev.sanastasov.bybon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.ui.theme.BybonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BybonTheme {
                BybonApp(DbModule.create(application))
            }
        }
    }
}