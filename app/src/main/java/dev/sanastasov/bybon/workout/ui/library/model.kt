package dev.sanastasov.bybon.workout.ui.library

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.label

data class ExerciseLibraryUiState(
    val groups: List<ExerciseLibraryGroup> = emptyList(),
) {
    val addEnabled: Boolean get() = groups.any { group -> group.exercises.any { it.selected } }
}

data class ExerciseLibraryGroup(
    val bodyPart: MuscleGroup,
    val exercises: List<ExerciseLibraryItemUi>,
)

data class ExerciseLibraryItemUi(
    val id: String,
    val name: String,
    val equipmentLabel: String,
    val selected: Boolean = false,
)

sealed class ExerciseLibraryAction {
    data class OnToggleExercise(
        val exerciseId: String,
    ) : ExerciseLibraryAction()

    data object OnAddExercise : ExerciseLibraryAction()
}

sealed class ExerciseLibraryEffect {
    data object NavigateBack : ExerciseLibraryEffect()
}

fun List<ExerciseDefinition>.groupedByBodyPart(
    selectedExerciseId: String? = null,
): List<ExerciseLibraryGroup> {
    val byGroup = groupBy { it.primaryMuscleGroup }
    return MuscleGroup.entries.mapNotNull { muscleGroup ->
        byGroup[muscleGroup]?.let { exercises ->
            ExerciseLibraryGroup(
                bodyPart = muscleGroup,
                exercises = exercises.map { it.toLibraryItem(selectedExerciseId) },
            )
        }
    }
}

private fun ExerciseDefinition.toLibraryItem(selectedExerciseId: String?): ExerciseLibraryItemUi =
    ExerciseLibraryItemUi(
        id = id,
        name = name,
        equipmentLabel = equipment.label,
        selected = id == selectedExerciseId,
    )
