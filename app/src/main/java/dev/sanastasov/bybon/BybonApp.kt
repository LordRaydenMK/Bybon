package dev.sanastasov.bybon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import dev.sanastasov.bybon.bodyweight.input.WeightInputScreen
import dev.sanastasov.bybon.main.MainModule
import dev.sanastasov.bybon.main.MainScreen
import dev.sanastasov.bybon.onermcalc.OneRmCalculatorScreen
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.notificationSummarySessionId
import dev.sanastasov.bybon.workout.ui.details.ExerciseDetailsScreen
import dev.sanastasov.bybon.workout.ui.library.EXERCISE_LIBRARY_RESULT_KEY
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

@Suppress("LongMethod")
@Composable
fun MainModule.BybonApp(openSession: OpenSessionRequest = OpenSessionRequest.None) {
    val backStack: BackStack = rememberNavBackStack(Screen.MainScreen)
    LaunchedEffect(openSession) {
        when (openSession) {
            OpenSessionRequest.None -> Unit

            is OpenSessionRequest.Open -> openWorkoutFromNotification(
                backStack,
                openSession.planId,
                workoutsRepository.notificationSummarySessionId(openSession.planId),
            )
        }
    }
    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            rememberResultEventBusNavEntryDecorator(),
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
                        onNavigateToSummary = { sessionId ->
                            backStack.add(Screen.WorkoutSummary(sessionId))
                        },
                        onNavigateToOneRmCalculator = { backStack.add(Screen.OneRmCalculator) },
                        onNavigateToExerciseLibrary = {
                            backStack.add(Screen.ExerciseLibrary(picker = false))
                        },
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
                        onNavigateToExerciseLibrary = { existingExerciseIds ->
                            backStack.add(Screen.ExerciseLibrary(existingExerciseIds))
                        },
                        onNavigateToExerciseDetails = { exerciseId ->
                            backStack.add(Screen.ExerciseDetails(exerciseId))
                        },
                    )
                }

                is Screen.EditPlan -> NavEntry(key) {
                    EditPlanScreen(
                        planId = key.planId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToExerciseLibrary = { existingExerciseIds ->
                            backStack.add(Screen.ExerciseLibrary(existingExerciseIds))
                        },
                    )
                }

                is Screen.ExerciseDetails -> NavEntry(key) {
                    ExerciseDetailsScreen(
                        exerciseId = key.exerciseId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                }

                is Screen.ExerciseLibrary -> NavEntry(key) {
                    val resultBus = LocalResultEventBus.current
                    ExerciseLibraryScreen(
                        existingExerciseIds = key.existingExerciseIds,
                        picker = key.picker,
                        onExercisePicked = { exerciseId ->
                            resultBus.sendResult(
                                resultKey = EXERCISE_LIBRARY_RESULT_KEY,
                                result = exerciseId,
                            )
                            backStack.removeLastOrNull()
                        },
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToExerciseDetails = { exerciseId ->
                            backStack.add(Screen.ExerciseDetails(exerciseId))
                        },
                    )
                }

                is Screen.WorkoutSession -> NavEntry(key) {
                    WorkoutSessionScreen(
                        key.planId,
                        onBack = { backStack.removeLastOrNull() },
                        onWorkoutCompleted = { sessionId ->
                            backStack.removeLastOrNull()
                            backStack.add(Screen.WorkoutSummary(sessionId))
                        },
                        onNavigateToExerciseLibrary = { existingExerciseIds ->
                            backStack.add(Screen.ExerciseLibrary(existingExerciseIds))
                        },
                        onNavigateToExerciseDetails = { exerciseId ->
                            backStack.add(Screen.ExerciseDetails(exerciseId))
                        },
                    )
                }

                Screen.WeightEntryScreen -> NavEntry(key) {
                    WeightInputScreen { backStack.removeLastOrNull() }
                }

                Screen.OneRmCalculator -> NavEntry(key) {
                    OneRmCalculatorScreen { backStack.removeLastOrNull() }
                }

                is Screen.WorkoutSummary -> NavEntry(key) {
                    WorkoutSummaryScreen(
                        sessionId = key.sessionId,
                        onNavigateBack = { backStack.removeLastOrNull() },
                        onNavigateToExerciseDetails = { exerciseId ->
                            backStack.add(Screen.ExerciseDetails(exerciseId))
                        },
                    )
                }
            }
        },
    )
}

internal fun openWorkoutSessionIfMissing(backStack: MutableList<Screen>, planId: WorkoutPlanId) {
    val target = Screen.WorkoutSession(planId)
    if (target !in backStack) {
        backStack.add(target)
    }
}

internal fun openWorkoutFromNotification(
    backStack: MutableList<Screen>,
    planId: WorkoutPlanId,
    completedSessionId: WorkoutSessionId?,
) {
    if (completedSessionId == null) {
        openWorkoutSessionIfMissing(backStack, planId)
        return
    }
    backStack.removeAll { screen ->
        screen is Screen.WorkoutSession && screen.planId == planId
    }
    val summary = Screen.WorkoutSummary(completedSessionId)
    if (summary !in backStack) {
        backStack.add(summary)
    }
}
