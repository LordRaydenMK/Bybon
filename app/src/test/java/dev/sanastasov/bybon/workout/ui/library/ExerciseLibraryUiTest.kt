package dev.sanastasov.bybon.workout.ui.library

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import org.junit.Test

class ExerciseLibraryUiTest {

    @Test
    fun `groups exercises by body part in enum order and skips empty groups`() {
        val chest = ExerciseDefinition("bench", "Bench", MuscleGroup.Chest, Equipment.Barbell)
        val arms = ExerciseDefinition("curl", "Curl", MuscleGroup.Arms, Equipment.Dumbbell)
        val core = ExerciseDefinition("plank", "Plank", MuscleGroup.Core, Equipment.Bodyweight)
        val groups = listOf(chest, core, arms).groupedByBodyPart()

        assert(
            groups.map { it.bodyPart } == listOf(
                MuscleGroup.Arms,
                MuscleGroup.Chest,
                MuscleGroup.Core,
            ),
        )
        assert(groups[0].exercises.single().name == "Curl")
        assert(groups[1].exercises.single().name == "Bench")
        assert(groups[2].exercises.single().name == "Plank")
    }

    @Test
    fun `keeps catalog order within a body part`() {
        val first = ExerciseDefinition("curl-db", "Curl DB", MuscleGroup.Arms, Equipment.Dumbbell)
        val second = ExerciseDefinition(
            "curl-machine",
            "Curl Machine",
            MuscleGroup.Arms,
            Equipment.Machine,
        )
        val groups = listOf(first, second).groupedByBodyPart()

        assert(groups.single().exercises.map { it.id } == listOf("curl-db", "curl-machine"))
    }

    @Test
    fun `assisted equipment uses a short label`() {
        val pullUp = ExerciseDefinition(
            "pullup-assisted",
            "Pull Up (assisted)",
            MuscleGroup.Back,
            Equipment.AssistedBodyWeight,
        )

        val item = listOf(pullUp).groupedByBodyPart().single().exercises.single()
        assert(item.equipment == Equipment.AssistedBodyWeight)
        assert(item.equipmentLabel == "Assisted")
    }

    @Test
    fun `other equipment uses the enum name`() {
        val bench = ExerciseDefinition(
            "bench-press-bb",
            "Bench Press (barbell)",
            MuscleGroup.Chest,
            Equipment.Barbell,
        )

        val item = listOf(bench).groupedByBodyPart().single().exercises.single()
        assert(item.equipment == Equipment.Barbell)
        assert(item.equipmentLabel == "Barbell")
    }

    @Test
    fun `marks only the selected exercise`() {
        val chest = ExerciseDefinition("bench", "Bench", MuscleGroup.Chest, Equipment.Barbell)
        val arms = ExerciseDefinition("curl", "Curl", MuscleGroup.Arms, Equipment.Dumbbell)
        val groups = listOf(chest, arms).groupedByBodyPart("curl")

        assert(groups[0].exercises.single().selected)
        assert(!groups[1].exercises.single().selected)
    }

    @Test
    fun `addEnabled is true when an exercise is selected`() {
        val curl = ExerciseDefinition("curl", "Curl", MuscleGroup.Arms, Equipment.Dumbbell)
        val state = listOf(curl).toLibraryUiState("curl")

        assert(state.addEnabled)
        assert(state.selectedExerciseId == "curl")
        assert(!state.showEmptyState)
    }

    @Test
    fun `addEnabled is false when nothing is selected`() {
        val curl = ExerciseDefinition("curl", "Curl", MuscleGroup.Arms, Equipment.Dumbbell)
        val state = listOf(curl).toLibraryUiState()

        assert(!state.addEnabled)
        assert(state.selectedExerciseId == null)
        assert(!state.groups.single().exercises.single().selected)
        assert(!state.showEmptyState)
    }

    @Test
    fun `default chips include every muscle group and equipment and omit clear all`() {
        val chips = ExerciseLibraryFilters().toChips()

        assert(chips.none { it.id == ExerciseLibraryFilterId.ClearAll })
        assert(
            chips.map { it.id } == MuscleGroup.entries.map {
                ExerciseLibraryFilterId.MuscleGroupFilter(it)
            } + Equipment.entries.map { ExerciseLibraryFilterId.EquipmentFilter(it) },
        )
        assert(chips.none { it.selected })
        val assisted = chips.single {
            it.id == ExerciseLibraryFilterId.EquipmentFilter(Equipment.AssistedBodyWeight)
        }
        assert(assisted.label == "Assisted")
    }

    @Test
    fun `clear all chip appears when any filter is selected`() {
        val chips = ExerciseLibraryFilters(muscleGroups = setOf(MuscleGroup.Arms)).toChips()

        assert(chips.first().id == ExerciseLibraryFilterId.ClearAll)
        assert(chips.first().label == "Clear all")
        assert(!chips.first().selected)
        val arms = chips.single {
            it.id == ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Arms)
        }
        assert(arms.selected)
        assert(chips.filter { it.selected }.single().id == arms.id)
    }

    @Test
    fun `toggling a selected chip deselects it`() {
        val filters = ExerciseLibraryFilters(muscleGroups = setOf(MuscleGroup.Arms))
            .toggle(ExerciseLibraryFilterId.MuscleGroupFilter(MuscleGroup.Arms))

        assert(filters == ExerciseLibraryFilters())
    }

    @Test
    fun `matching with no filters returns every exercise`() {
        val exercises = listOf(curl, bench, squat)

        assert(exercises.matching(ExerciseLibraryFilters()) == exercises)
    }

    @Test
    fun `muscle group chips are OR`() {
        val matches = listOf(curl, bench, squat).matching(
            ExerciseLibraryFilters(muscleGroups = setOf(MuscleGroup.Arms, MuscleGroup.Legs)),
        )

        assert(matches == listOf(curl, squat))
    }

    @Test
    fun `equipment chips are OR`() {
        val matches = listOf(curl, bench, squat).matching(
            ExerciseLibraryFilters(equipment = setOf(Equipment.Dumbbell, Equipment.Barbell)),
        )

        assert(matches == listOf(curl, bench, squat))
    }

    @Test
    fun `muscle group and equipment chips are AND`() {
        val matches = listOf(curl, machineCurl, bench, squat).matching(
            ExerciseLibraryFilters(
                muscleGroups = setOf(MuscleGroup.Arms),
                equipment = setOf(Equipment.Dumbbell),
            ),
        )

        assert(matches == listOf(curl))
    }

    @Test
    fun `empty matching filters show the empty state`() {
        val state = listOf(curl, bench).toLibraryUiState(
            filters = ExerciseLibraryFilters(muscleGroups = setOf(MuscleGroup.Core)),
        )

        assert(state.groups.isEmpty())
        assert(state.showEmptyState)
        assert(state.filterChips.first().id == ExerciseLibraryFilterId.ClearAll)
    }

    @Test
    fun `selected exercise remains selected in state when filtered out`() {
        val state = listOf(curl, bench).toLibraryUiState(
            selectedExerciseId = "curl",
            filters = ExerciseLibraryFilters(muscleGroups = setOf(MuscleGroup.Chest)),
        )

        assert(state.selectedExerciseId == "curl")
        assert(state.addEnabled)
        assert(state.existingExercises.single().id == "curl")
        assert(state.existingExercises.single().selected)
        assert(state.groups.single().exercises.none { it.selected })
        assert(state.groups.single().exercises.none { it.id == "curl" })
    }

    @Test
    fun `existing exercises appear under already added and leave muscle groups`() {
        val state = listOf(curl, bench, squat).toLibraryUiState(
            existingExerciseIds = listOf("bench"),
        )

        assert(state.existingHeader == "Already added")
        assert(state.existingExercises.single().id == "bench")
        assert(state.existingExercises.single().name == "Bench")
        assert(!state.existingExercises.single().selectable)
        assert(!state.existingExercises.single().selected)
        assert(
            state.groups.map { it.bodyPart } == listOf(MuscleGroup.Arms, MuscleGroup.Legs),
        )
        assert(state.groups.flatMap { it.exercises }.none { it.id == "bench" })
    }

    @Test
    fun `selected catalog exercise is appended to existing and can be unchecked`() {
        val state = listOf(curl, bench, squat).toLibraryUiState(
            selectedExerciseId = "curl",
            existingExerciseIds = listOf("bench"),
        )

        assert(state.existingExercises.map { it.id } == listOf("bench", "curl"))
        assert(!state.existingExercises[0].selectable)
        assert(!state.existingExercises[0].selected)
        assert(state.existingExercises[1].selectable)
        assert(state.existingExercises[1].selected)
        assert(state.groups.flatMap { it.exercises }.none { it.id == "curl" })
        assert(state.addEnabled)
    }

    @Test
    fun `selecting an existing exercise is ignored`() {
        val state = listOf(curl, bench).toLibraryUiState(
            selectedExerciseId = "bench",
            existingExerciseIds = listOf("bench"),
        )

        assert(state.selectedExerciseId == null)
        assert(!state.addEnabled)
        assert(state.existingExercises.single().id == "bench")
        assert(!state.existingExercises.single().selectable)
        assert(!state.existingExercises.single().selected)
        assert(state.groups.single().exercises.single().id == "curl")
    }

    @Test
    fun `empty matching filters still show existing exercises`() {
        val state = listOf(curl, bench).toLibraryUiState(
            filters = ExerciseLibraryFilters(muscleGroups = setOf(MuscleGroup.Core)),
            existingExerciseIds = listOf("bench"),
        )

        assert(state.showEmptyState)
        assert(state.groups.isEmpty())
        assert(state.existingHeader == "Already added")
        assert(state.existingExercises.single().id == "bench")
    }

    @Test
    fun `clear all resets both categories`() {
        val filters = ExerciseLibraryFilters(
            muscleGroups = setOf(MuscleGroup.Arms),
            equipment = setOf(Equipment.Dumbbell),
        ).toggle(ExerciseLibraryFilterId.ClearAll)

        assert(filters == ExerciseLibraryFilters())
    }

    private val curl = ExerciseDefinition("curl", "Curl", MuscleGroup.Arms, Equipment.Dumbbell)
    private val machineCurl = ExerciseDefinition(
        "curl-machine",
        "Curl Machine",
        MuscleGroup.Arms,
        Equipment.Machine,
    )
    private val bench = ExerciseDefinition("bench", "Bench", MuscleGroup.Chest, Equipment.Barbell)
    private val squat = ExerciseDefinition("squat", "Squat", MuscleGroup.Legs, Equipment.Barbell)
}
