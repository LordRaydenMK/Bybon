@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sanastasov.bybon.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import dev.sanastasov.bybon.onermcalc.OneRmCalcAction
import dev.sanastasov.bybon.onermcalc.OneRmCalculatorTab
import dev.sanastasov.bybon.onermcalc.OneRmCalculatorViewModel
import dev.sanastasov.bybon.onermcalc.OneRmEntry
import dev.sanastasov.bybon.onermcalc.OneRmUiState
import dev.sanastasov.bybon.ui.icons.FontAwesomeWeight
import dev.sanastasov.bybon.ui.icons.TablerBarbell
import dev.sanastasov.bybon.weight.WeightModule
import dev.sanastasov.bybon.weight.dashboard.WeeklyAverageEntryUi
import dev.sanastasov.bybon.weight.dashboard.WeightDashboardTab
import dev.sanastasov.bybon.weight.dashboard.WeightDashboardUiState
import dev.sanastasov.bybon.weight.dashboard.WeightDashboardViewModel
import dev.sanastasov.bybon.weight.dashboard.WeightEntryUi

@Composable
fun WeightModule.MainScreen(onNavigateToWeightEntry: () -> Unit) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val oneRmViewModel = retain { OneRmCalculatorViewModel() }
    val weight by oneRmViewModel.weight
    val reps by oneRmViewModel.reps
    val oneRmUiState by oneRmViewModel.uiState.collectAsStateWithLifecycle()

    val weightViewModel = retain {
        WeightDashboardViewModel(weightRepository, it.coroutineScope)
    }
    val weightState by weightViewModel.uiState.collectAsStateWithLifecycle()

    MainScreenContent(
        selectedIndex,
        { selectedIndex = it },
        weight,
        reps,
        oneRmUiState,
        oneRmViewModel::onAction,
        weightState,
        onNavigateToWeightEntry,
    )
}

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
) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Bybon") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selectedIndex == 0,
                    { onTabSelected(0) },
                    icon = {
                        Icon(TablerBarbell, null)
                    },
                    label = { Text("1 RM Calc") }
                )
                NavigationBarItem(
                    selectedIndex == 1,
                    { onTabSelected(1) },
                    icon = {
                        Icon(FontAwesomeWeight, null)
                    },
                    label = { Text("Weight") }
                )
            }
        }
    ) { contentPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (selectedIndex) {
                0 -> OneRmCalculatorTab(
                    weight,
                    reps,
                    oneRmUiState,
                    onOneRmAction,
                    Modifier.fillMaxSize()
                )

                1 -> WeightDashboardTab(weightState, onLogWeightClicked)
                else -> error("Not yet implemented")
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenContentOneRmCalcPreview() {
    MainScreenContent(
        0,
        {},
        "50",
        "10",
        OneRmUiState(
            OneRmEntry(50f, 10),
            emptyList()
        ),
        {},
        WeightDashboardUiState(true, emptyList(), emptyList()),
        {}
    )
}

@Preview
@Composable
private fun MainScreenContentWeightTrackPreview() {
    MainScreenContent(
        1,
        {},
        "50",
        "10",
        OneRmUiState(
            OneRmEntry(50f, 10),
            emptyList()
        ),
        {},
        WeightDashboardUiState(
            true,
            listOf(
                WeightEntryUi("Yesterday", "65.2kg"),
                WeightEntryUi("Monday", "65.2kg"),
            ),
            listOf(
                WeeklyAverageEntryUi("CW 32", "64.8 kg", "+0.1 vs CW 31"),
                WeeklyAverageEntryUi("CW 31", "64.7 kg", "same as CW 30"),
            )
        ),
        {}
    )
}
