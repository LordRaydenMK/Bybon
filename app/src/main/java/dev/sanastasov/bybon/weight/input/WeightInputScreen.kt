@file:OptIn(ExperimentalMaterial3Api::class)

package dev.sanastasov.bybon.weight.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun WeightInputScreen() {
    WeightInputContent()
}

@Composable
private fun WeightInputContent() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Enter Weight") }) }
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding)) {
            Text("Today")
            TextField("", {})
            Button({}) {
                Text("Save")
            }
        }
    }
}

@Preview
@Composable
private fun WeightInputContentPreview() {
    WeightInputContent()
}