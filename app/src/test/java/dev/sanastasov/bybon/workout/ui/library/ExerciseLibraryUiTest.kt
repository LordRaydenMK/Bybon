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
        val state = ExerciseLibraryUiState(listOf(curl).groupedByBodyPart("curl"))

        assert(state.addEnabled)
        assert(state.selectedExerciseId == "curl")
    }

    @Test
    fun `addEnabled is false when nothing is selected`() {
        val curl = ExerciseDefinition("curl", "Curl", MuscleGroup.Arms, Equipment.Dumbbell)
        val state = ExerciseLibraryUiState(listOf(curl).groupedByBodyPart())

        assert(!state.addEnabled)
        assert(state.selectedExerciseId == null)
        assert(!state.groups.single().exercises.single().selected)
    }
}
