@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sanastasov.bybon.weight.input

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.icons.SavedLocally
import dev.sanastasov.bybon.weight.BodyWeight
import dev.sanastasov.bybon.weight.WeightModule
import java.time.LocalDate

private val OneHundredGrams = BodyWeight(10)
private val FiftyGrams = BodyWeight(5)

@Composable
fun WeightModule.WeightInputScreen() {
    val viewModel = retain {
        WeightInputViewModel(weightRepository, it.coroutineScope)
    }
    val weight by viewModel.weight
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeightInputContent(weight, uiState, viewModel::onAction)
}

@Composable
private fun WeightInputContent(
    weight: String,
    uiState: WeightInputUi,
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton({ onAction(WeightInputAction.OnNewDateSelected(uiState.previousDate)) }) {
                    Text("<")
                }
                if (uiState.date == LocalDate.now()) {
                    Text("Today")
                } else {
                    Text(uiState.date.toString())
                }
                OutlinedButton(
                    { onAction(WeightInputAction.OnNewDateSelected(uiState.nextDate!!)) },
                    enabled = uiState.nextDate != null
                ) {
                    Text(">")
                }
            }

            Row(Modifier.fillMaxWidth()) {
                if (uiState.saved) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Image(SavedLocally, "Saved in db")
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                TextField(
                    weight,
                    { onAction(WeightInputAction.OnWeightChanged(it)) },
                    Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType =
                            KeyboardType.DecimalSigned
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onAction(WeightInputAction.OnSaveWeight(uiState.date, weight))
                        }
                    )
                )
                Spacer(Modifier.size(48.dp))
            }

            AdjustWeightRow(onAction)

            Button({ onAction(WeightInputAction.OnSaveWeight(uiState.date, weight)) }) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun AdjustWeightRow(onAction: (WeightInputAction) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton({ onAction(WeightInputAction.OnUpdateWeight(-OneHundredGrams)) }) {
            Text("-0.1")
        }
        OutlinedButton({ onAction(WeightInputAction.OnUpdateWeight(-FiftyGrams)) }) {
            Text("-0.05")
        }
        OutlinedButton({ onAction(WeightInputAction.OnUpdateWeight(FiftyGrams)) }) {
            Text("+0.05")
        }
        OutlinedButton({ onAction(WeightInputAction.OnUpdateWeight(OneHundredGrams)) }) {
            Text("+0.1")
        }
    }
}

@Preview
@Composable
private fun WeightInputContentPreview() {
    WeightInputContent("", WeightInputUi(LocalDate.now(), false)) {}
}

@Preview
@Composable
private fun WeightInputSavedContentPreview() {
    WeightInputContent("", WeightInputUi(LocalDate.now(), true)) {}
}