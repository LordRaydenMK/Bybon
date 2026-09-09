package dev.sanastasov.bybon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import dev.sanastasov.bybon.bodyweight.input.WeightInputScreen
import dev.sanastasov.bybon.main.MainModule
import dev.sanastasov.bybon.main.MainScreen
import dev.sanastasov.bybon.workout.ui.session.WorkoutSessionScreen

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
fun MainModule.BybonApp() {
    val backStack: BackStack = rememberNavBackStack(Screen.MainScreen)
    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                Screen.MainScreen -> NavEntry(key) {
                    MainScreen(
                        onNavigateToWeightEntry = { backStack.add(Screen.WeightEntryScreen) },
                        onNavigateToStartSession = { backStack.add(Screen.WorkoutSession(it.id)) }
                    )
                }

                is Screen.WorkoutSession -> NavEntry(key) {
                    WorkoutSessionScreen(key.planId)
                }

                Screen.WeightEntryScreen -> NavEntry(key) {
                    WeightInputScreen { backStack.removeLastOrNull() }
                }
            }
        }
    )
}
