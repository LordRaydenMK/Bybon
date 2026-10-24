@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sanastasov.bybon.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.icons.FontAwesomeWeight
import dev.sanastasov.bybon.ui.icons.MaterialSymbolsExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.ui.history.AndroidContentResolverReader
import dev.sanastasov.bybon.workout.ui.history.WorkoutHistoryAction
import dev.sanastasov.bybon.workout.ui.history.WorkoutHistoryTab
import dev.sanastasov.bybon.workout.ui.history.WorkoutHistoryUiState
import dev.sanastasov.bybon.workout.ui.history.WorkoutHistoryViewModel
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlanEffect
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlanUi
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlansAction
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlansUiState
import dev.sanastasov.bybon.workout.ui.plans.WorkoutPlansViewModel
import dev.sanastasov.bybon.workout.ui.plans.WorkoutsTab
import java.time.LocalDate

@Composable
fun MainModule.MainScreen(
    onNavigateToWeightEntry: () -> Unit,
    onNavigateToStartSession: (WorkoutPlan) -> Unit,
    onNavigateToOverview: (WorkoutPlan) -> Unit,
    onNavigateToEditPlan: (WorkoutPlan) -> Unit,
    onNavigateToSummary: (WorkoutSessionId) -> Unit,
    onNavigateToOneRmCalculator: () -> Unit,
    onNavigateToExerciseLibrary: () -> Unit,
) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

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

    val contentResolver = LocalContext.current.contentResolver
    val historyViewModel = retain {
        WorkoutHistoryViewModel(
            workoutsRepository,
            it.coroutineScope,
            AndroidContentResolverReader(contentResolver),
        )
    }
    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        historyViewModel.onAction(WorkoutHistoryAction.OnCsvSelected(uri))
    }

    MainScreenContent(
        selectedIndex,
        { selectedIndex = it },
        weightState,
        onNavigateToWeightEntry,
        plansState,
        workoutPlansViewModel::onAction,
        historyState,
        {
            documentPicker.launch(
                arrayOf(
                    "*/*",
                    "text/*",
                    "text/csv",
                    "text/comma-separated-values",
                    "application/csv",
                ),
            )
        },
        { historyViewModel.onAction(WorkoutHistoryAction.OnImportDone) },
        onNavigateToSummary,
        onNavigateToOneRmCalculator,
        onNavigateToExerciseLibrary,
    )
}

@Suppress("LongMethod")
@Composable
private fun MainScreenContent(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    weightState: WeightDashboardUiState,
    onLogWeightClicked: () -> Unit,
    plans: WorkoutPlansUiState,
    onWorkoutPlansAction: (WorkoutPlansAction) -> Unit,
    historyState: WorkoutHistoryUiState,
    onImportHistoryClick: () -> Unit,
    onImportDone: () -> Unit,
    onSessionClick: (WorkoutSessionId) -> Unit,
    onNavigateToOneRmCalculator: () -> Unit,
    onNavigateToExerciseLibrary: () -> Unit,
) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Bybon") },
                actions = {
                    MainOverflowMenu(
                        onNavigateToOneRmCalculator,
                        onNavigateToExerciseLibrary,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selectedIndex == 0,
                    { onTabSelected(0) },
                    icon = {
                        Icon(MaterialSymbolsExercise, "Workout Plans tab")
                    },
                    label = { Text("Workout Plans") },
                )
                NavigationBarItem(
                    selectedIndex == 1,
                    { onTabSelected(1) },
                    icon = {
                        Icon(Icons.Filled.DateRange, "History tab")
                    },
                    label = { Text("History") },
                )
                NavigationBarItem(
                    selectedIndex == 2,
                    { onTabSelected(2) },
                    icon = {
                        Icon(FontAwesomeWeight, "Weight Tracking tab")
                    },
                    label = { Text("Weight Tracking") },
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

                1 -> WorkoutHistoryTab(
                    historyState,
                    onSessionClick,
                    onImportHistoryClick,
                    onImportDone,
                    Modifier.fillMaxSize(),
                )

                2 -> WeightDashboardTab(weightState, onLogWeightClicked)

                else -> error("Not yet implemented")
            }
        }
    }
}

@Composable
private fun MainOverflowMenu(
    onNavigateToOneRmCalculator: () -> Unit,
    onNavigateToExerciseLibrary: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Overflow menu")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("1RM calculator") },
                onClick = {
                    expanded = false
                    onNavigateToOneRmCalculator()
                },
            )
            DropdownMenuItem(
                text = { Text("Exercise library") },
                onClick = {
                    expanded = false
                    onNavigateToExerciseLibrary()
                },
            )
        }
    }
}

@Preview
@Composable
private fun MainScreenContentWorkoutsPreview() {
    MainScreenContent(
        0,
        {},
        WeightDashboardUiState(LogWeightPrompt.Prominent, null, emptyList(), emptyList()),
        {},
        WorkoutPlansUiState(
            listOf(
                WorkoutPlanUi(fullBodyA, isActive = false),
                WorkoutPlanUi(fullBodyB, isActive = false),
            ),
        ),
        {},
        WorkoutHistoryUiState.Empty,
        {},
        {},
        {},
        {},
        {},
    )
}

@Preview
@Composable
private fun MainScreenContentHistoryPreview() {
    MainScreenContent(
        1,
        {},
        WeightDashboardUiState(LogWeightPrompt.Prominent, null, emptyList(), emptyList()),
        {},
        WorkoutPlansUiState(
            listOf(
                WorkoutPlanUi(fullBodyA, isActive = false),
                WorkoutPlanUi(fullBodyB, isActive = false),
            ),
        ),
        {},
        WorkoutHistoryUiState.Empty,
        {},
        {},
        {},
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
        WorkoutPlansUiState(
            listOf(
                WorkoutPlanUi(fullBodyA, isActive = false),
                WorkoutPlanUi(fullBodyB, isActive = false),
            ),
        ),
        {},
        WorkoutHistoryUiState.Empty,
        {},
        {},
        {},
        {},
        {},
    )
}
