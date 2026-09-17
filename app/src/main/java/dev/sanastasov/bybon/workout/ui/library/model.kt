package dev.sanastasov.bybon.workout.ui.library

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.label

data class ExerciseLibraryUiState(
    val groups: List<ExerciseLibraryGroup> = emptyList(),
)

data class ExerciseLibraryGroup(
    val bodyPart: MuscleGroup,
    val exercises: List<ExerciseLibraryItemUi>,
)

data class ExerciseLibraryItemUi(
    val id: String,
    val name: String,
    val equipmentLabel: String,
)

fun List<ExerciseDefinition>.groupedByBodyPart(): List<ExerciseLibraryGroup> {
    val byGroup = groupBy { it.primaryMuscleGroup }
    return MuscleGroup.entries.mapNotNull { muscleGroup ->
        byGroup[muscleGroup]?.let { exercises ->
            ExerciseLibraryGroup(
                bodyPart = muscleGroup,
                exercises = exercises.map { it.toLibraryItem() },
            )
        }
    }
}

private fun ExerciseDefinition.toLibraryItem(): ExerciseLibraryItemUi = ExerciseLibraryItemUi(
    id = id,
    name = name,
    equipmentLabel = equipment.label,
)
