package dev.sanastasov.bybon.workout.domain.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB

@Composable
fun WorkoutPlansScreen() {

}

@Composable
private fun WorkoutPlansContent(plans: List<WorkoutPlan>) {
    Scaffold(
        topBar = { BybonTopAppBar("Workout Plans", {}) }
    ) { contentPadding ->
        LazyColumn(
            Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = contentPadding
        ) {
            items(plans) { plan ->
                WorkoutPlanCard(plan)
            }
        }
    }
}

@Composable
private fun WorkoutPlanCard(plan: WorkoutPlan) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp), Arrangement.spacedBy(4.dp)) {
            Text(plan.name, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            plan.description?.let {
                Text(it)
            }

            plan.exercises.forEach { (exercise, sets, repRange) ->
                Text("$sets x ${exercise.name} for ${repRange.first} to ${repRange.last}")
            }
        }
    }
}

@Preview
@Composable
private fun WorkoutPlansContentPreview() {
    WorkoutPlansContent(listOf(fullBodyA, fullBodyB))
}