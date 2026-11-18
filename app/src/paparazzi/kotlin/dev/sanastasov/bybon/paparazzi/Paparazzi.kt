package dev.sanastasov.bybon.paparazzi

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import dev.sanastasov.bybon.ui.theme.BybonTheme

fun Paparazzi.bybonSnapshot(
    name: String? = null,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
): Unit = snapshot(name) {
    BybonTheme(darkTheme = darkTheme, dynamicColor = false) {
        content()
    }
}
