package dev.sanastasov.bybon.workout.domain

internal val bybonCatalogExercises = listOf(
    ExerciseDefinition(
        id = "iso-lat-row",
        name = "Iso-Lateral Row (machine)",
        bodyPart = BodyPart.Back,
        equipment = Equipment.Machine,
        mechanic = Mechanic.Compound,
        primaryMuscles = listOf("latissimus_dorsi", "rhomboids"),
        secondaryMuscles = listOf("biceps_brachii", "posterior_deltoid"),
        equipmentSlug = "iso_lateral_row_machine",
    ),
    ExerciseDefinition(
        id = "squat-machine",
        name = "Squat (machine)",
        bodyPart = BodyPart.UpperLegs,
        equipment = Equipment.Machine,
        mechanic = Mechanic.Compound,
        primaryMuscles = listOf("gluteus_maximus", "quadriceps"),
        secondaryMuscles = listOf("erector_spinae", "hamstrings"),
        equipmentSlug = "squat_machine",
    ),
    ExerciseDefinition(
        id = "triceps-press-machine",
        name = "Triceps Press (machine)",
        bodyPart = BodyPart.UpperArms,
        equipment = Equipment.Machine,
        mechanic = Mechanic.Isolation,
        primaryMuscles = listOf("triceps_brachii"),
        equipmentSlug = "tricep_press_machine",
    ),
    ExerciseDefinition(
        id = "lateral-raise-machine",
        name = "Lateral Raise (machine)",
        bodyPart = BodyPart.Shoulders,
        equipment = Equipment.Machine,
        mechanic = Mechanic.Isolation,
        primaryMuscles = listOf("lateral_deltoid"),
        secondaryMuscles = listOf("anterior_deltoid", "trapezius"),
        equipmentSlug = "lateral_raise_machine",
    ),
)

internal val catalogExercises = repdbCatalogExercises + bybonCatalogExercises

internal fun catalogExercise(id: String): ExerciseDefinition = catalogExercisesById.getValue(id)

private val catalogExercisesById = catalogExercises.associateBy { it.id }
