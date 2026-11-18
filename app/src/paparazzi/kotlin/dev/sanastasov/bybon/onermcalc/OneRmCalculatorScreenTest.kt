package dev.sanastasov.bybon.onermcalc

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.paparazzi.bybonSnapshot
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import org.junit.Rule
import org.junit.Test

class OneRmCalculatorScreenTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_9,
        theme = "android:Theme.Material.Light.NoActionBar",
    )

    @Test
    fun empty() {
        paparazzi.bybonSnapshot {
            OneRmCalculatorContent(
                weight = "",
                reps = "",
                uiState = OneRmUiState(entry = null),
            )
        }
    }

    @Test
    fun resultAndHistory() {
        paparazzi.bybonSnapshot {
            OneRmCalculatorContent(
                weight = "50",
                reps = "10",
                uiState = OneRmUiState(
                    OneRmEntry(50f, 10),
                    listOf(
                        OneRmEntry(50f, 9),
                        OneRmEntry(50f, 10),
                        OneRmEntry(52.5f, 8),
                        OneRmEntry(52.5f, 9),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun OneRmCalculatorContent(
    weight: String,
    reps: String,
    uiState: OneRmUiState,
) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { BybonTopAppBar("1RM calculator", {}) },
    ) { contentPadding ->
        OneRmCalculatorTab(
            weight,
            reps,
            uiState,
            {},
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}
