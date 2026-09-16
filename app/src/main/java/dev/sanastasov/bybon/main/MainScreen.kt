@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sanastasov.bybon.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.bodyweight.BodyWeight
import dev.sanastasov.bybon.bodyweight.BodyWeightEntry
import dev.sanastasov.bybon.bodyweight.dashboard.LogWeightPrompt
import dev.sanastasov.bybon.bodyweight.dashboard.WeeklyAverageEntryUi
import dev.sanastasov.bybon.bodyweight.dashboard.WeightDashboardTab
import dev.sanastasov.bybon.bodyweight.dashboard.WeightDashboardUiState
import dev.sanastasov.bybon.bodyweight.dashboard.WeightDashboardViewModel
import dev.sanastasov.bybon.onermcalc.OneRmCalcAction
import dev.sanastasov.bybon.onermcalc.OneRmCalculatorTab
import dev.sanastasov.bybon.onermcalc.OneRmCalculatorViewModel
import dev.sanastasov.bybon.onermcalc.OneRmEntry
import dev.sanastasov.bybon.onermcalc.OneRmUiState
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.icons.FontAwesomeWeight
import dev.sanastasov.bybon.ui.icons.MaterialSymbolsExercise
import dev.sanastasov.bybon.ui.icons.TablerBarbell
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlanEffect
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlanUi
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlansAction
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlansViewModel
import dev.sanastasov.bybon.workout.ui.plans.WorkoutsTab
import java.time.LocalDate

@Composable
fun MainModule.MainScreen(
    onNavigateToWeightEntry: () -> Unit,
    onNavigateToDietPhase: () -> Unit,
    onNavigateToStartSession: (WorkoutPlan) -> Unit,
    onNavigateToOverview: (WorkoutPlan) -> Unit,
    onNavigateToEditPlan: (WorkoutPlan) -> Unit,
    onNavigateToHistory: () -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val oneRmViewModel = retain { OneRmCalculatorViewModel() }
    val weight by oneRmViewModel.weight
    val reps by oneRmViewModel.reps
    val oneRmUiState by oneRmViewModel.uiState.collectAsStateWithLifecycle()

    val weightViewModel = retain {
        WeightDashboardViewModel(bodyWeightRepository, it.coroutineScope)
    }
    val weightState by weightViewModel.uiState.collectAsStateWithLifecycle()

    val workoutPlansViewModel = retain {
        WorkoutPlansViewModel(workoutsRepository, it.coroutineScope)
    }
    val plansState by workoutPlansViewModel.uiState.collectAsStateWithLifecycle()
    workoutPlansViewModel.effects.collectEffectWithLifecycle { effect ->
        when (effect) {
            is WorkoutPlanEffect.OpenOverview -> onNavigateToOverview(effect.plan)
            is WorkoutPlanEffect.OpenSession -> onNavigateToStartSession(effect.plan)
            is WorkoutPlanEffect.OpenEditPlan -> onNavigateToEditPlan(effect.plan)
        }
    }

    MainScreenContent(
        selectedIndex,
        { selectedIndex = it },
        weight,
        reps,
        oneRmUiState,
        oneRmViewModel::onAction,
        weightState,
        onNavigateToWeightEntry,
        onNavigateToDietPhase,
        plansState,
        workoutPlansViewModel::onAction,
        onNavigateToHistory,
    )
}

@Suppress("LongMethod")
@Composable
private fun MainScreenContent(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    weight: String,
    reps: String,
    oneRmUiState: OneRmUiState,
    onOneRmAction: (OneRmCalcAction) -> Unit,
    weightState: WeightDashboardUiState,
    onLogWeightClicked: () -> Unit,
    onDietPhaseClicked: () -> Unit,
    plans: List<WorkoutPlanUi>,
    onWorkoutPlansAction: (WorkoutPlansAction) -> Unit,
    onHistoryClicked: () -> Unit,
) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Bybon") },
                actions = {
                    if (selectedIndex == 0) {
                        IconButton(onHistoryClicked) {
                            Icon(Icons.Filled.DateRange, contentDescription = "History")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selectedIndex == 0,
                    { onTabSelected(0) },
                    icon = {
                        Icon(MaterialSymbolsExercise, "Workouts tab")
                    },
                    label = { Text("Workouts") },
                )
                NavigationBarItem(
                    selectedIndex == 1,
                    { onTabSelected(1) },
                    icon = {
                        Icon(TablerBarbell, "One RM calculator tab")
                    },
                    label = { Text("1 RM Calc") },
                )
                NavigationBarItem(
                    selectedIndex == 2,
                    { onTabSelected(2) },
                    icon = {
                        Icon(FontAwesomeWeight, "Body Weight tab")
                    },
                    label = { Text("Weight") },
                )
            }
        },
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (selectedIndex) {
                0 -> WorkoutsTab(plans, onWorkoutPlansAction, Modifier.fillMaxSize())

                1 -> OneRmCalculatorTab(
                    weight,
                    reps,
                    oneRmUiState,
                    onOneRmAction,
                    Modifier.fillMaxSize(),
                )

                2 -> WeightDashboardTab(weightState, onLogWeightClicked, onDietPhaseClicked)

                else -> error("Not yet implemented")
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenContentWorkoutsPreview() {
    MainScreenContent(
        0,
        {},
        "50",
        "10",
        OneRmUiState(null, emptyList()),
        {},
        WeightDashboardUiState(LogWeightPrompt.Prominent, null, emptyList(), emptyList()),
        {},
        {},
        listOf(
            WorkoutPlanUi(fullBodyA, isActive = false),
            WorkoutPlanUi(fullBodyB, isActive = false),
        ),
        {},
        {},
    )
}

@Preview
@Composable
private fun MainScreenContentOneRmCalcPreview() {
    MainScreenContent(
        1,
        {},
        "50",
        "10",
        OneRmUiState(
            OneRmEntry(50f, 10),
            emptyList(),
        ),
        {},
        WeightDashboardUiState(LogWeightPrompt.Prominent, null, emptyList(), emptyList()),
        {},
        {},
        listOf(
            WorkoutPlanUi(fullBodyA, isActive = false),
            WorkoutPlanUi(fullBodyB, isActive = false),
        ),
        {},
        {},
    )
}

@Preview
@Composable
private fun MainScreenContentWeightTrackPreview() {
    MainScreenContent(
        2,
        {},
        "50",
        "10",
        OneRmUiState(
            OneRmEntry(50f, 10),
            emptyList(),
        ),
        {},
        WeightDashboardUiState(
            LogWeightPrompt.Prominent,
            null,
            listOf(
                BodyWeightEntry(LocalDate.now().minusDays(1), BodyWeight.parseFromString("65.2")),
                BodyWeightEntry(LocalDate.now().minusDays(2), BodyWeight.parseFromString("64.8")),
            ),
            listOf(
                WeeklyAverageEntryUi("CW 32", "64.8 kg", "+0.1 vs CW 31"),
                WeeklyAverageEntryUi("CW 31", "64.7 kg", "same as CW 30"),
            ),
        ),
        {},
        {},
        listOf(
            WorkoutPlanUi(fullBodyA, isActive = false),
            WorkoutPlanUi(fullBodyB, isActive = false),
        ),
        {},
        {},
    )
}
