package dev.sanastasov.bybon.workout.ui.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import dev.marcellogalhardo.retained.compose.retain
import dev.sanastasov.bybon.ui.collectEffectWithLifecycle
import dev.sanastasov.bybon.ui.components.BybonTopAppBar
import dev.sanastasov.bybon.workout.WorkoutModule
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutSessionId
import dev.sanastasov.bybon.workout.domain.canRemoveExercise
import dev.sanastasov.bybon.workout.domain.completeSet
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.restTimer
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import dev.sanastasov.bybon.workout.ui.ExerciseCard
import dev.sanastasov.bybon.workout.ui.ExerciseCardEvent
import dev.sanastasov.bybon.workout.ui.ExerciseCardMode
import dev.sanastasov.bybon.workout.ui.library.EXERCISE_LIBRARY_RESULT_KEY
import java.time.LocalDateTime
import kotlin.time.Duration

@Composable
fun WorkoutModule.WorkoutSessionScreen(
    planId: WorkoutPlanId,
    onBack: () -> Unit,
    onWorkoutCompleted: (WorkoutSessionId) -> Unit,
    onNavigateToExerciseLibrary: (List<String>) -> Unit,
    onNavigateToExerciseDetails: (String) -> Unit,
) {
    val viewModel = retain {
        WorkoutSessionViewModel(planId, workoutsRepository, it.coroutineScope)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resultBus = LocalResultEventBus.current
    ResultEffect<String>(resultKey = EXERCISE_LIBRARY_RESULT_KEY) { exerciseId ->
        resultBus.removeResult(resultKey = EXERCISE_LIBRARY_RESULT_KEY)
        viewModel.onAction(WorkoutSessionAction.OnExercisePicked(exerciseId))
    }
    uiState?.let { session ->
        val pagerState = rememberPagerState(
            session.currentExerciseIndex ?: 0,
        ) { session.exercises.size }
        viewModel.effects.collectEffectWithLifecycle { effect ->
            when (effect) {
                WorkoutSessionEffect.NavigateBack -> onBack()

                is WorkoutSessionEffect.NavigateToSummary -> onWorkoutCompleted(effect.sessionId)

                is WorkoutSessionEffect.OpenExerciseLibrary ->
                    onNavigateToExerciseLibrary(effect.existingExerciseIds)

                is WorkoutSessionEffect.OpenExerciseDetails ->
                    onNavigateToExerciseDetails(effect.exerciseId)

                is WorkoutSessionEffect.ScrollToExercise -> {
                    if (pagerState.pageCount == 0) return@collectEffectWithLifecycle
                    pagerState.animateScrollToPage(
                        effect.page.coerceIn(0, pagerState.pageCount - 1),
                    )
                }
            }
        }
        SessionScreenContent(
            session,
            pagerState,
            viewModel::onAction,
            onBack,
        )
    }
}

@Composable
private fun SessionScreenContent(
    state: WorkoutSession,
    pagerState: PagerState,
    onAction: (WorkoutSessionAction) -> Unit,
    onBack: () -> Unit,
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            BybonTopAppBar(
                state.planName,
                onBack,
                actions = {
                    SessionElapsedClock(state.startedAt)
                    SessionOverflowMenu(
                        onAddExercise = { onAction(WorkoutSessionAction.OnAddExercise) },
                        onCancelWorkout = { showCancelDialog = true },
                    )
                },
            )
        },
    ) { contentPadding ->
        Column(
            Modifier
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxSize(),
        ) {
            state.note?.let {
                Text(it)
                Spacer(Modifier.height(8.dp))
            }
            SessionExercisePager(
                state,
                pagerState,
                onAction,
                Modifier.weight(1f),
            )
        }
    }
    if (showCancelDialog) {
        CancelWorkoutDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = {
                showCancelDialog = false
                onAction(WorkoutSessionAction.OnCancelWorkout)
            },
        )
    }
}

@Composable
private fun SessionExercisePager(
    state: WorkoutSession,
    pagerState: PagerState,
    onAction: (WorkoutSessionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        HorizontalPager(
            pagerState,
            Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            key = { page -> state.exercises.getOrNull(page)?.id ?: "page-$page" },
        ) { page ->
            val exercise = state.exercises.getOrNull(page) ?: return@HorizontalPager
            Card(Modifier.fillMaxWidth()) {
                ExerciseCard(
                    exercise = exercise,
                    mode = ExerciseCardMode.Session,
                    onEvent = { event -> onAction(event.toSessionAction(exercise)) },
                    onRemoveExercise = if (state.canRemoveExercise(exercise)) {
                        { onAction(WorkoutSessionAction.OnRemoveExercise(exercise)) }
                    } else {
                        null
                    },
                    onTitleClick = {
                        onAction(WorkoutSessionAction.OnOpenExerciseDetails(exercise.id))
                    },
                    isLastExercise = page == state.exercises.lastIndex,
                    activeRest = state.restTimer(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Exercise ${pagerState.currentPage + 1} / ${state.exercises.size}")
    }
}

@Composable
private fun SessionOverflowMenu(onAddExercise: () -> Unit, onCancelWorkout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton({ expanded = true }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "More options for workout",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Add Exercise") },
                onClick = {
                    expanded = false
                    onAddExercise()
                },
            )
            DropdownMenuItem(
                text = { Text("Cancel workout") },
                onClick = {
                    expanded = false
                    onCancelWorkout()
                },
            )
        }
    }
}

@Composable
private fun CancelWorkoutDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel workout?") },
        text = { Text("This discards the workout and all logged sets.") },
        confirmButton = {
            TextButton(onConfirm) { Text("Discard") }
        },
        dismissButton = {
            TextButton(onDismiss) { Text("Keep workout") }
        },
    )
}

private fun ExerciseCardEvent.toSessionAction(exercise: WorkoutExercise): WorkoutSessionAction =
    when (this) {
        ExerciseCardEvent.OnIncrease -> WorkoutSessionAction.OnIncreaseExercise(exercise)

        ExerciseCardEvent.OnDecrease -> WorkoutSessionAction.OnDecreaseExercise(exercise)

        ExerciseCardEvent.OnResetAllSets -> WorkoutSessionAction.OnResetExercise(exercise)

        is ExerciseCardEvent.OnResetSet ->
            WorkoutSessionAction.OnResetSet(exercise, index, isWarmup)

        is ExerciseCardEvent.OnWeightUpdated ->
            WorkoutSessionAction.OnWeightUpdated(weight, exercise, index, isWarmup)

        is ExerciseCardEvent.OnRepsUpdated ->
            WorkoutSessionAction.OnRepsUpdated(reps, exercise, index, isWarmup)

        ExerciseCardEvent.OnAddSet -> WorkoutSessionAction.OnAddSet(exercise)

        ExerciseCardEvent.OnRemoveLastSet -> WorkoutSessionAction.RemoveLastSet(exercise)

        ExerciseCardEvent.OnConvertToWarmup -> WorkoutSessionAction.OnConvertToWarmup(exercise)

        ExerciseCardEvent.OnConvertToWorkSet -> WorkoutSessionAction.OnConvertToWorkSet(exercise)

        is ExerciseCardEvent.OnCompleteSet ->
            WorkoutSessionAction.OnCompleteSet(exercise, index, isWarmup)
    }

@Preview
@Composable
private fun SessionScreenContentPage1CompletedExercisePreview() {
    val session = fullBodyA.toWorkoutSession()
    SessionScreenContentPreview(
        session.completeSet(session.exercises.first(), 0, isWarmup = true),
    )
}

@Preview
@Composable
private fun SessionScreenContentRestAfterLastWarmupPreview() {
    val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0)
    val completedAt = startedAt.plusMinutes(3)
    val session = (0..2).fold(fullBodyA.toWorkoutSession(startedAt = startedAt)) { current, index ->
        current.completeSet(current.exercises.first(), index, isWarmup = true, now = completedAt)
    }
    SessionScreenContentPreview(session)
}

@Preview
@Composable
private fun SessionScreenContentInitialPreview() {
    SessionScreenContentPreview(fullBodyA.toWorkoutSession())
}

@Preview
@Composable
private fun SessionScreenContentWithPreviousPreview() {
    val previous = fullBodyA.toWorkoutSession().let { session ->
        session.copy(
            exercises = session.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(40 + index),
                            reps = 9,
                            setState = SetState.Completed(),
                        )
                    },
                    warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(22 + index),
                            reps = 6,
                            setState = SetState.Completed(),
                        )
                    },
                )
            },
            startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
            duration = Duration.ZERO,
        )
    }
    SessionScreenContentPreview(fullBodyA.toWorkoutSession(previous))
}

@Composable
private fun SessionScreenContentPreview(state: WorkoutSession) {
    SessionScreenContent(
        state,
        rememberPagerState(state.currentExerciseIndex ?: 0) { state.exercises.size },
        {},
        {},
    )
}
