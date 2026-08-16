package dev.sanastasov.bybon.onermcalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain

@Composable
fun OneRmCalculatorScreen() {
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
    OneRmCalculatorContent(
        weight,
        reps,
        entry,
        history,
        viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneRmCalculatorContent(
    weight: String,
    reps: String,
    entry: OneRmEntry?,
    history: List<OneRmEntry>,
    onAction: (OneRmCalcAction) -> Unit
) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("1 RM Calculator") }) }
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeightSection(
                weight,
                { onAction(OneRmCalcAction.OnWeightChanged(it)) },
                { onAction(OneRmCalcAction.OnUpdateWeight(it)) }
            )

            Spacer(Modifier.height(16.dp))

            RepsSection(
                reps,
                { onAction(OneRmCalcAction.OnRepsChanged(it)) },
                { onAction(OneRmCalcAction.OnUpdateReps(it)) }
            )

            entry?.calculate1Rm()?.let {
                Spacer(Modifier.height(16.dp))

                ResultColumn(it)
            }

            Spacer(Modifier.height(16.dp))

            HistorySection(history)
        }
    }
}

@Composable
private fun WeightSection(
    weight: String,
    onWeightChanged: (String) -> Unit,
    onUpdateWeight: (Float) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Text("Weight", Modifier.weight(1f))
        OutlinedTextField(
            weight,
            onWeightChanged,
            Modifier.weight(3f),
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        OutlinedButton({ onUpdateWeight(-5f) }) {
            Text("-5kg")
        }
        OutlinedButton({ onUpdateWeight(-2.5f) }) {
            Text("-2.5kg")
        }
        OutlinedButton({ onUpdateWeight(2.5f) }) {
            Text("+2.5kg")
        }
        OutlinedButton({ onUpdateWeight(5f) }) {
            Text("+5kg")
        }
    }
}

@Composable
private fun RepsSection(
    reps: String,
    onRepsChanged: (String) -> Unit,
    onUpdateReps: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Text("Reps", Modifier.weight(1f))
        OutlinedTextField(
            reps,
            { onRepsChanged(it) },
            Modifier.weight(3f),
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            )
        )
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        OutlinedButton({ onUpdateReps(-1) }) {
            Text("-1 rep")
        }
        OutlinedButton({ onUpdateReps(1) }) {
            Text("+1 rep")
        }
    }
}

@Composable
private fun ResultColumn(oneRm: Float) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "%.2f".format(oneRm),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Estimated 1RM")
    }
}

@Composable
private fun HistorySection(items: List<OneRmEntry>) {
    items.forEach { entry ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${entry.weight} kg x", Modifier.weight(1f))
            Text("${entry.reps} reps", Modifier.weight(1f))
            val oneRM = ("%.2f").format(entry.calculate1Rm())
            Text("~ 1 RM: $oneRM", Modifier.weight(1f))
        }
    }
}

@Preview
@Composable
fun OneRmCalculatorPreview() {
    OneRmCalculatorContent(
        "50",
        "10",
        OneRmEntry(50f, 10),
        listOf(
            OneRmEntry(50f, 9),
            OneRmEntry(50f, 10),
            OneRmEntry(52.5f, 8),
            OneRmEntry(52.5f, 9),
        ),
        {},
    )
}