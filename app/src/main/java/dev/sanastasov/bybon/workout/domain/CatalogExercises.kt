package dev.sanastasov.bybon.workout.domain

internal val bybonCatalogExercises = listOf(
    ExerciseDefinition(
        "iso-lat-row",
        "Iso-Lateral Row (machine)",
        MuscleGroup.Back,
        Equipment.Machine,
        Mechanic.Compound,
    ),
    ExerciseDefinition(
        "squat-machine",
        "Squat (machine)",
        MuscleGroup.Legs,
        Equipment.Machine,
        Mechanic.Compound,
    ),
    ExerciseDefinition(
        "triceps-press-machine",
        "Triceps Press (machine)",
        MuscleGroup.Arms,
        Equipment.Machine,
        Mechanic.Isolation,
    ),
    ExerciseDefinition(
        "lateral-raise-machine",
        "Lateral Raise (machine)",
        MuscleGroup.Shoulders,
        Equipment.Machine,
        Mechanic.Isolation,
    ),
)

internal val catalogExercises = repdbCatalogExercises + bybonCatalogExercises

internal fun catalogExercise(id: String): ExerciseDefinition = catalogExercisesById.getValue(id)

private val catalogExercisesById = catalogExercises.associateBy { it.id }
