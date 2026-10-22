package dev.sanastasov.bybon.workout.ui.details

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseImages
import dev.sanastasov.bybon.workout.domain.label
import dev.sanastasov.bybon.workout.domain.toCatalogLabel

sealed class ExerciseDetailsUiState {
    data object Loading : ExerciseDetailsUiState()
    data object Missing : ExerciseDetailsUiState()
    data class Content(
        val name: String,
        val images: ExerciseImages? = null,
        val facts: List<String> = emptyList(),
        val primaryMuscles: List<String> = emptyList(),
        val secondaryMuscles: List<String> = emptyList(),
        val description: String? = null,
        val instructions: List<String> = emptyList(),
        val tips: List<String> = emptyList(),
        val tags: List<String> = emptyList(),
    ) : ExerciseDetailsUiState()
}

internal fun ExerciseDefinition.toDetailsUi(): ExerciseDetailsUiState.Content =
    ExerciseDetailsUiState.Content(
        name = name,
        images = images,
        facts = listOfNotNull(
            bodyPart?.label,
            equipment?.label,
            mechanic?.name,
            forceType?.name,
            difficulty?.name,
        ),
        primaryMuscles = primaryMuscles.map { it.toCatalogLabel() },
        secondaryMuscles = secondaryMuscles.map { it.toCatalogLabel() },
        description = description,
        instructions = instructions.orEmpty(),
        tips = tips.orEmpty(),
        tags = tags.map { it.toCatalogLabel() },
    )
