package dev.sanastasov.bybon.workout.ui.library

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.label

const val EXERCISE_LIBRARY_RESULT_KEY = "exercise_id"

private const val ALREADY_ADDED_HEADER = "Already added"

data class ExerciseLibraryUiState(
    val existingExercises: List<ExerciseLibraryItemUi> = emptyList(),
    val groups: List<ExerciseLibraryGroup> = emptyList(),
    val filterChips: List<ExerciseLibraryFilterChipUi> = emptyList(),
    val selectedExerciseId: String? = null,
) {
    val addEnabled: Boolean get() = selectedExerciseId != null

    val showEmptyState: Boolean get() = groups.isEmpty() && filterChips.any { it.selected }

    val existingHeader: String?
        get() = if (existingExercises.isNotEmpty()) ALREADY_ADDED_HEADER else null

    fun isExisting(exerciseId: String): Boolean =
        existingExercises.any { it.id == exerciseId && !it.selectable }
}

data class ExerciseLibraryGroup(
    val bodyPart: MuscleGroup,
    val exercises: List<ExerciseLibraryItemUi>,
)

data class ExerciseLibraryItemUi(
    val id: String,
    val name: String,
    val equipment: Equipment,
    val selected: Boolean = false,
    val selectable: Boolean = true,
) {
    val equipmentLabel: String get() = equipment.label
}

data class ExerciseLibraryFilterChipUi(
    val id: ExerciseLibraryFilterId,
    val label: String,
    val selected: Boolean,
)

sealed interface ExerciseLibraryFilterId {
    data object ClearAll : ExerciseLibraryFilterId

    data class MuscleGroupFilter(
        val muscleGroup: MuscleGroup,
    ) : ExerciseLibraryFilterId

    data class EquipmentFilter(
        val equipment: Equipment,
    ) : ExerciseLibraryFilterId
}

data class ExerciseLibraryFilters(
    val muscleGroups: Set<MuscleGroup> = emptySet(),
    val equipment: Set<Equipment> = emptySet(),
) {
    val isActive: Boolean get() = muscleGroups.isNotEmpty() || equipment.isNotEmpty()

    fun toggle(id: ExerciseLibraryFilterId): ExerciseLibraryFilters = when (id) {
        ExerciseLibraryFilterId.ClearAll -> ExerciseLibraryFilters()

        is ExerciseLibraryFilterId.MuscleGroupFilter -> copy(
            muscleGroups = muscleGroups.toggle(id.muscleGroup),
        )

        is ExerciseLibraryFilterId.EquipmentFilter -> copy(
            equipment = equipment.toggle(id.equipment),
        )
    }
}

sealed class ExerciseLibraryAction {
    data class OnToggleExercise(
        val exerciseId: String,
    ) : ExerciseLibraryAction()

    data class OnAddExercise(
        val exerciseId: String,
    ) : ExerciseLibraryAction()

    data class OnToggleFilter(
        val id: ExerciseLibraryFilterId,
    ) : ExerciseLibraryAction()
}

sealed class ExerciseLibraryEffect {
    data class ExercisePicked(
        val exerciseId: String,
    ) : ExerciseLibraryEffect()
}

fun List<ExerciseDefinition>.toLibraryUiState(
    selectedExerciseId: String? = null,
    filters: ExerciseLibraryFilters = ExerciseLibraryFilters(),
    existingExerciseIds: List<String> = emptyList(),
): ExerciseLibraryUiState {
    val existingIdSet = existingExerciseIds.toSet()
    val pendingId = selectedExerciseId.takeUnless { it in existingIdSet }
    return ExerciseLibraryUiState(
        existingExercises = existingExerciseIds.toExistingItems(this, pendingId),
        groups = matching(filters)
            .filter { it.id !in existingIdSet && it.id != pendingId }
            .groupedByBodyPart(pendingId),
        filterChips = filters.toChips(),
        selectedExerciseId = pendingId,
    )
}

private fun List<String>.toExistingItems(
    catalog: List<ExerciseDefinition>,
    pendingId: String?,
): List<ExerciseLibraryItemUi> {
    val byId = catalog.associateBy { it.id }
    val items = mapNotNull { id ->
        byId[id]?.toLibraryItem(selectedExerciseId = null, selectable = false)
    }
    val pending = pendingId?.let { id -> byId[id]?.toLibraryItem(id) }
    return if (pending != null) items + pending else items
}

fun List<ExerciseDefinition>.matching(filters: ExerciseLibraryFilters): List<ExerciseDefinition> =
    filter { exercise ->
        val matchesMuscle =
            filters.muscleGroups.isEmpty() || exercise.primaryMuscleGroup in filters.muscleGroups
        val matchesEquipment =
            filters.equipment.isEmpty() || exercise.equipment in filters.equipment
        matchesMuscle && matchesEquipment
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

fun ExerciseLibraryFilters.toChips(): List<ExerciseLibraryFilterChipUi> = buildList {
    if (isActive) {
        add(
            ExerciseLibraryFilterChipUi(
                id = ExerciseLibraryFilterId.ClearAll,
                label = "Clear all",
                selected = false,
            ),
        )
    }
    MuscleGroup.entries.forEach { muscleGroup ->
        add(
            ExerciseLibraryFilterChipUi(
                id = ExerciseLibraryFilterId.MuscleGroupFilter(muscleGroup),
                label = muscleGroup.name,
                selected = muscleGroup in muscleGroups,
            ),
        )
    }
    Equipment.entries.forEach { equipment ->
        add(
            ExerciseLibraryFilterChipUi(
                id = ExerciseLibraryFilterId.EquipmentFilter(equipment),
                label = equipment.label,
                selected = equipment in this@toChips.equipment,
            ),
        )
    }
}

private fun ExerciseDefinition.toLibraryItem(
    selectedExerciseId: String?,
    selectable: Boolean = true,
): ExerciseLibraryItemUi = ExerciseLibraryItemUi(
    id = id,
    name = name,
    equipment = equipment,
    selected = selectable && id == selectedExerciseId,
    selectable = selectable,
)

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
