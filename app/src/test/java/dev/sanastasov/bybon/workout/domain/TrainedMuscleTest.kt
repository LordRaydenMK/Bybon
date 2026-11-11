package dev.sanastasov.bybon.workout.domain

import org.junit.Test

class TrainedMuscleTest {

    @Test
    fun `maps primary muscles and keeps display order`() {
        val muscles = listOf(
            catalogExercise("bench-press"),
            catalogExercise("iso-lat-row"),
            catalogExercise("squat-machine"),
            catalogExercise("seated-leg-curl"),
            catalogExercise("lateral-raise-machine"),
            catalogExercise("incline-db-curl"),
            catalogExercise("triceps-press-machine"),
        ).flatMap { it.trainedMuscles() }
            .toSet()
            .sortedBy { it.ordinal }

        assert(
            muscles == listOf(
                TrainedMuscle.Chest,
                TrainedMuscle.Back,
                TrainedMuscle.Quads,
                TrainedMuscle.Hamstrings,
                TrainedMuscle.Glutes,
                TrainedMuscle.Shoulders,
                TrainedMuscle.Biceps,
                TrainedMuscle.Triceps,
            ),
        )
    }

    @Test
    fun `falls back to body part when primary muscles are missing`() {
        val definition = catalogExercise("bench-press").copy(primaryMuscles = emptyList())
        assert(definition.trainedMuscles() == setOf(TrainedMuscle.Chest))
    }

    @Test
    fun `maps every catalog primary muscle or documents it as ignored`() {
        val ignored = setOf(
            "abductors",
            "adductors",
            "hip_flexors",
            "neck",
            "sternocleidomastoid",
        )
        val unknown = catalogExercises.flatMap { it.primaryMuscles }.toSet()
            .filter { it.toTrainedMuscle() == null && it !in ignored }

        assert(unknown.isEmpty()) { "Unmapped primary muscles: $unknown" }
    }
}
