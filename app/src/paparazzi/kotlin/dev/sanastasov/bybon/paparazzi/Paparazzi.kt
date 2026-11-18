package dev.sanastasov.bybon.paparazzi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.Paparazzi
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import dev.sanastasov.bybon.ui.LocalSkipChartAnimation
import dev.sanastasov.bybon.ui.theme.BybonTheme

fun Paparazzi.bybonSnapshot(
    name: String? = null,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
): Unit = snapshot(name) {
    CompositionLocalProvider(LocalSkipChartAnimation provides true) {
        BybonTheme(darkTheme = darkTheme, dynamicColor = false) {
            content()
        }
    }
}

@OptIn(DelicateCoilApi::class)
fun Paparazzi.installFakeCoil(engine: FakeImageLoaderEngine): Unit {
    SingletonImageLoader.setUnsafe(
        ImageLoader.Builder(context)
            .components { add(engine) }
            .build(),
    )
}
