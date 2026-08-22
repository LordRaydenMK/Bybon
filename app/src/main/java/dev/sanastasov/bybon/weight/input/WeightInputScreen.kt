@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sanastasov.bybon.weight.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.weight.InMemoryWeightRepository

@Composable
fun WeightInputScreen() {
    val viewModel = retain {
        WeightInputViewModel(
            InMemoryWeightRepository,
            it.coroutineScope
        )
    }
    val weight by viewModel.weight
    WeightInputContent(weight, viewModel::onAction)
}

@Composable
private fun WeightInputContent(
    weight: String,
    onAction: (WeightInputAction) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Enter Weight") }) }
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Today")

            TextField(
                weight,
                { onAction(WeightInputAction.OnWeightChanged(it)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.DecimalSigned),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onAction(WeightInputAction.OnSaveWeight(weight))
                    }
                )
            )

            Button({ onAction(WeightInputAction.OnSaveWeight(weight)) }) {
                Text("Save")
            }
        }
    }
}

@Preview
@Composable
private fun WeightInputContentPreview() {
    WeightInputContent("", {})
}