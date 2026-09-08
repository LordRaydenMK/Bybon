package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionAction
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession

@Composable
fun WorkoutModule.WorkoutSessionScreen(planId: WorkoutPlanId) {
    val viewModel = retain {
        WorkoutSessionViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    uiState?.let {
        SessionScreenContent(it, viewModel::onAction)
    }
}

@Composable
private fun SessionScreenContent(
    state: WorkoutSession,
    onAction: (WorkoutSessionAction) -> Unit,
) {
    Scaffold(
        topBar = { BybonTopAppBar(state.planName, {}) }
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            state.planDescription?.let {
                Text(it)
                Spacer(Modifier.height(8.dp))
            }
            val pagerState = rememberPagerState(0) {
                state.exercises.size
            }
            HorizontalPager(pagerState) {
                Card {
                    val exercise = state.exercises.elementAt(pagerState.currentPage)
                    ExerciseCard(
                        exercise,
                        { exercise, _ -> onAction(WorkoutSessionAction.OnCompleteSet(exercise)) })
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Exercise ${pagerState.currentPage + 1} / ${state.exercises.size}")
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: WorkoutExercise,
    onCompleteSet: (WorkoutExercise, Int) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "${exercise.sets.size} x ${exercise.exerciseDefinition.name} in ${exercise.repRange.first} - ${exercise.repRange.last}",
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))

        exercise.sets.forEachIndexed { index, (definition, weight, reps, state) ->
            Row(
                Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state == SetState.InProgress) {
                    Text(
                        "${index + 1}. ${weight.kilograms} kg x ${exercise.repRange.first}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Text("${index + 1}. ${weight.kilograms} kg x ${exercise.repRange.first}")
                }
                Spacer(Modifier.weight(1f))
                when (state) {
                    SetState.InProgress -> Checkbox(
                        false,
                        { onCompleteSet(exercise, index) }
                    )

                    SetState.Completed -> Checkbox(
                        true,
                        null,
                        Modifier.padding(horizontal = 10.dp)
                    )

                    SetState.NotStated -> {

                    }
                }
            }

            if (index == exercise.sets.lastIndex) {
                TextButton({}) {
                    Text("Add set")
                }
            }
        }
    }
}

@Preview
@Composable
private fun SessionScreenContentPage1CompletedExercisePreview() {
    SessionScreenContent(fullBodyA.toWorkoutSession().completeSet(), {})
}

@Preview
@Composable
private fun SessionScreenContentPage4Preview() {
    SessionScreenContent(fullBodyA.toWorkoutSession(), {})
}
