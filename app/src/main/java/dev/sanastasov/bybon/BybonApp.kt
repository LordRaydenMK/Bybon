package dev.sanastasov.bybon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.main.MainScreen
import dev.sanastasov.bybon.weight.WeightModule
import dev.sanastasov.bybon.weight.input.WeightInputScreen

typealias BackStack = NavBackStack<Screen>

@Composable
fun <T : NavKey> rememberNavBackStack(vararg elements: NavKey): NavBackStack<T> {
    return rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer())
    ) {
        @Suppress("UNCHECKED_CAST")
        NavBackStack(*elements) as NavBackStack<T>
    }
}

@Composable
fun BybonApp(dbModule: DbModule) {
    val backStack: BackStack = rememberNavBackStack(Screen.MainScreen)
    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator()
        ),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            with(WeightModule.create(dbModule)) {
                when (key) {
                    Screen.MainScreen -> NavEntry(key) {
                        MainScreen { backStack.add(Screen.WeightEntryScreen) }
                    }

                    Screen.WeightEntryScreen -> NavEntry(key) {
                        WeightInputScreen()
                    }
                }
            }
        }
    )
}
