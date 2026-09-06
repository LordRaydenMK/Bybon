package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.fullBodyA

@Composable
fun WorkoutModule.WorkoutSessionScreen(planId: WorkoutPlanId) {
    val viewModel = retain {
        WorkoutSessionViewModel(planId, workoutPlansRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    uiState?.let {
        SessionScreenContent(it)
    }
}

@Composable
private fun SessionScreenContent(state: WorkoutSessionUiState) {
    Scaffold(
        topBar = { BybonTopAppBar(state.plan.name, {}) }
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            state.plan.description?.let {
                Text(it)
                Spacer(Modifier.height(8.dp))
            }
            val pagerState = rememberPagerState(state.currentPage) {
                state.plan.setsByExercise.size
            }
            HorizontalPager(pagerState) {
                Card {
                    val set = state.plan.setsByExercise.values.elementAt(state.currentPage)
                    ExerciseCard(set)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Exercise ${state.currentPage + 1} / ${state.plan.setsByExercise.size}")
        }
    }
}

@Composable
private fun ExerciseCard(set: List<ExerciseSet>) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val exerciseSet = set.first()
        Text("${set.size} x ${exerciseSet.exercise.name} in ${exerciseSet.repRange.first} - ${exerciseSet.repRange.last}")
        set.forEachIndexed { index, (_, repRange) ->
            Text("${index + 1}. 50 kg x ${repRange.first}")

            if (index == set.lastIndex) {
                TextButton({}) {
                    Text("Add set")
                }
            }
        }
    }
}

@Preview
@Composable
private fun SessionScreenContentPage1Preview() {
    SessionScreenContent(WorkoutSessionUiState(fullBodyA, 0))
}

@Preview
@Composable
private fun SessionScreenContentPage4Preview() {
    SessionScreenContent(WorkoutSessionUiState(fullBodyA, 4))
}
