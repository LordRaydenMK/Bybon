package dev.sanastasov.bybon.onermcalc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun OneRmCalculatorScreen() {
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    val oneRm by remember {
        derivedStateOf {
            if (weight.isBlank() || reps.isBlank()) null
            else calculate1Rm(weight.toFloat(), reps.toInt())
        }
    }
    OneRmCalculatorContent(
        weight,
        { weight = it },
        reps,
        { reps = it },
        oneRm
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneRmCalculatorContent(
    weight: String,
    onWeightChanged: (String) -> Unit,
    reps: String,
    onRepsChanged: (String) -> Unit,
    oneRm: Float?,
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
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weight")
                TextField(
                    weight,
                    onWeightChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reps")
                TextField(
                    reps,
                    onRepsChanged,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Text("Estimated 1RM: $oneRm kg")
        }
    }
}

@Preview
@Composable
fun OneRmCalculatorPreview() {
    OneRmCalculatorContent("50", {}, "10", {}, 72f)
}