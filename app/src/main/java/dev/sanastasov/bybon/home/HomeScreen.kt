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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.sanastasov.bybon.ui.icons.FontAwesomeWeight
import dev.sanastasov.bybon.ui.icons.TablerBarbell

@Composable
fun HomeScreen() {
    var selectedIndex by remember { mutableIntStateOf(0) }

    HomeScreenContent(selectedIndex)
}

@Composable
private fun HomeScreenContent(selectedIndex: Int) {
    Scaffold(
        Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Bybon") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selectedIndex == 0,
                    {},
                    icon = {
                        Icon(TablerBarbell, null)
                    },
                    label = { Text("1 RM Calc") }
                )
                NavigationBarItem(
                    selectedIndex == 1,
                    {},
                    icon = {
                        Icon(FontAwesomeWeight, null)
                    },
                    label = { Text("Weight") }
                )
            }
        }
    ) { contentPadding ->
        Box(Modifier.padding(contentPadding)) {
            when (selectedIndex) {
                0 -> Text("1 RM Calculator")
                1 -> Text("Weight Tracking")
                else -> error("Not yet implemented")
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenContentPreview() {
    HomeScreenContent(0)
}
