@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sanastasov.bybon.home

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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.onermcalc.OneRmCalcAction
import dev.sanastasov.bybon.onermcalc.OneRmCalculatorContent
import dev.sanastasov.bybon.onermcalc.OneRmCalculatorViewModel
import dev.sanastasov.bybon.onermcalc.OneRmEntry
import dev.sanastasov.bybon.ui.icons.FontAwesomeWeight
import dev.sanastasov.bybon.ui.icons.TablerBarbell

@Composable
fun MainScreen() {
    val viewModel = retain { OneRmCalculatorViewModel() }
    val weight by viewModel.weight
    val reps by viewModel.reps
    val entry by remember {
        derivedStateOf {
            weight.toFloatOrNull()?.let { weight ->
                reps.toIntOrNull()?.let { reps ->
                    OneRmEntry(weight, reps)
                }
            }
        }
    }
    val history by viewModel.uiState.collectAsStateWithLifecycle()


    var selectedIndex by remember { mutableIntStateOf(0) }

    MainScreenContent(
        selectedIndex,
        { selectedIndex = it },
        weight,
        reps,
        entry,
        history,
        viewModel::onAction,
    )
}

@Composable
private fun MainScreenContent(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    weight: String,
    reps: String,
    entry: OneRmEntry?,
    history: List<OneRmEntry>,
    onOneRmAction: (OneRmCalcAction) -> Unit,
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
                0 -> OneRmCalculatorContent(
                    weight,
                    reps,
                    entry,
                    history,
                    onOneRmAction
                )

                1 -> Text("Weight Tracking (WIP)")
                else -> error("Not yet implemented")
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenContentPreview() {
    MainScreenContent(
        0,
        {},
        "50",
        "10",
        OneRmEntry(50f, 10),
        emptyList(),
        {}
    )
}
