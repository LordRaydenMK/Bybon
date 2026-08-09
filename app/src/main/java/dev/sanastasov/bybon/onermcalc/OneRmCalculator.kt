package dev.sanastasov.bybon.onermcalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OneRmCalculatorScreen() {
    var weight by rememberSaveable { mutableStateOf("50") }
    var reps by rememberSaveable { mutableStateOf("10") }
    val entry by remember {
        derivedStateOf {
            weight.toFloatOrNull()?.let { weight ->
                reps.toIntOrNull()?.let { reps ->
                    OneRmEntry(weight, reps)
                }
            }
        }
    }
    OneRmCalculatorContent(
        weight,
        reps,
        entry,
        onWeightChanged = { weight = it },
        onRepsChanged = { reps = it },
        onUpdateReps = { repsToAdd ->
            reps = (reps.toInt() + repsToAdd).toString()
        },
        onUpdateWeight = { weightToAdd ->
            weight = (weight.toFloat() + weightToAdd).toString()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneRmCalculatorContent(
    weight: String,
    reps: String,
    entry: OneRmEntry?,
    onWeightChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onUpdateWeight: (Float) -> Unit
) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("1 RM Calculator") }) }
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WeightSection(weight, onWeightChanged, onUpdateWeight)

            Spacer(Modifier.height(16.dp))

            RepsSection(reps, onRepsChanged, onUpdateReps)

            entry?.calculate1Rm()?.let {
                Spacer(Modifier.height(16.dp))

                ResultColumn(it)
            }
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
        Text("Weight")
        OutlinedTextField(
            weight,
            onWeightChanged,
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
        Text("Reps")
        OutlinedTextField(
            reps,
            { onRepsChanged(it) },
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

@Preview
@Composable
fun OneRmCalculatorPreview() {
    OneRmCalculatorContent(
        "50",
        "10",
        OneRmEntry(50f, 10),
        {},
        {},
        {},
        {}
    )
}