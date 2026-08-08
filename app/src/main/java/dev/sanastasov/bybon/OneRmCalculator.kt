package dev.sanastasov.bybon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun OneRmCalculatorScreen() {
    OneRmCalculatorContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneRmCalculatorContent() {
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
                TextField("", {})
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reps")
                TextField("", {})
            }

            Button({}) {
                Text("Calculate")
            }
        }
    }
}

@Preview
@Composable
fun OneRmCalculatorPreview() {
    OneRmCalculatorContent()
}