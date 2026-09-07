package dev.sanastasov.bybon.workout.ui.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB

@Composable
fun WorkoutPlansScreen() {

}

@Composable
fun WorkoutsTab(
    plans: List<WorkoutPlan>,
    onAction: (WorkoutPlansAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(plans) { plan ->
            WorkoutPlanCard(plan) { onAction(WorkoutPlansAction.OnStartPlan(it)) }
        }
    }
}

@Composable
private fun WorkoutPlanCard(plan: WorkoutPlan, onStartWorkoutClicked: (WorkoutPlan) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            Arrangement.spacedBy(4.dp)
        ) {
            Text(plan.name, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            plan.description?.let {
                Text(it, fontSize = 14.sp)
            }

            plan.sets.forEach { planedSet ->
                Text("${planedSet.sets} x ${planedSet.exercise.name} - ${planedSet.repRange.first} to ${planedSet.repRange.last} reps")
            }

            TextButton(
                { onStartWorkoutClicked(plan) },
                Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Start Workout")
            }
        }
    }
}

@Preview
@Composable
private fun WorkoutPlansContentPreview() {
    Surface {
        WorkoutsTab(listOf(fullBodyA, fullBodyB), {})
    }
}