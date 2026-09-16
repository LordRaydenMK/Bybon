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
import dev.sanastasov.bybon.workout.ui.history.WorkoutHistoryScreen
import dev.sanastasov.bybon.workout.ui.library.ExerciseLibraryScreen
import dev.sanastasov.bybon.workout.ui.overview.WorkoutOverviewScreen
import dev.sanastasov.bybon.workout.ui.plans.EditPlanScreen
import dev.sanastasov.bybon.workout.ui.session.WorkoutSessionScreen
import dev.sanastasov.bybon.workout.ui.summary.WorkoutSummaryScreen

typealias BackStack = NavBackStack<Screen>

@Composable
fun <T : NavKey> rememberNavBackStack(vararg elements: NavKey): NavBackStack<T> =
    rememberSerializable(
        serializer = NavBackStackSerializer(elementSerializer = NavKeySerializer()),
    ) {
        @Suppress("UNCHECKED_CAST")
        NavBackStack(*elements) as NavBackStack<T>
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
                        onNavigateToStartSession = { backStack.add(Screen.WorkoutSession(it.id)) },
                        onNavigateToOverview = { backStack.add(Screen.WorkoutOverview(it.id)) },
                        onNavigateToEditPlan = { backStack.add(Screen.EditPlan(it.id)) },
                        onNavigateToHistory = { backStack.add(Screen.WorkoutHistory) },
                    )
                }

                is Screen.WorkoutOverview -> NavEntry(key) {
                    WorkoutOverviewScreen(
                        planId = key.planId,
                        onBack = { backStack.removeLastOrNull() },
                        onStartSession = {
                            backStack.removeLastOrNull()
                            backStack.add(Screen.WorkoutSession(key.planId))
                        },
                    )
                }

                is Screen.EditPlan -> NavEntry(key) {
                    EditPlanScreen(
                        planId = key.planId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToExerciseLibrary = {
                            backStack.add(Screen.ExerciseLibrary(key.planId))
                        },
                    )
                }

                is Screen.ExerciseLibrary -> NavEntry(key) {
                    ExerciseLibraryScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                }

                is Screen.WorkoutSession -> NavEntry(key) {
                    WorkoutSessionScreen(key.planId)
                }

                Screen.WeightEntryScreen -> NavEntry(key) {
                    WeightInputScreen { backStack.removeLastOrNull() }
                }

                Screen.WorkoutHistory -> NavEntry(key) {
                    WorkoutHistoryScreen(
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToSummary = { sessionId ->
                            backStack.add(Screen.WorkoutSummary(sessionId))
                        },
                    )
                }

                is Screen.WorkoutSummary -> NavEntry(key) {
                    WorkoutSummaryScreen(
                        sessionId = key.sessionId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                }
            }
        },
    )
}
