package dev.sanastasov.bybon.workout.domain

internal val catalogExercises = listOf(
    // Arms
    ExerciseDefinition(
        "incline-curl-db",
        "Incline Curl (dumbbell)",
        MuscleGroup.Arms,
        Equipment.Dumbbell,
    ),
    ExerciseDefinition(
        "biceps-curl-machine",
        "Curl (machine)",
        MuscleGroup.Arms,
        Equipment.Machine,
    ),
    ExerciseDefinition(
        "skullcrusher-db",
        "Skullcrusher (dumbbell)",
        MuscleGroup.Arms,
        Equipment.Dumbbell,
    ),
    ExerciseDefinition(
        "triceps-press-machine",
        "Triceps Press (machine)",
        MuscleGroup.Arms,
        Equipment.Machine,
    ),
    // Back
    ExerciseDefinition(
        "incline-row-db",
        "Incline Row (dumbbell)",
        MuscleGroup.Back,
        Equipment.Dumbbell,
    ),
    ExerciseDefinition(
        "pullup-assisted",
        "Pull Up (assisted)",
        MuscleGroup.Back,
        Equipment.AssistedBodyWeight,
    ),
    ExerciseDefinition(
        "lat-pull-down",
        "Lat Pull-down (cable)",
        MuscleGroup.Back,
        Equipment.Machine,
    ),
    ExerciseDefinition(
        "iso-lat-row",
        "Iso-Lateral Row (machine)",
        MuscleGroup.Back,
        Equipment.Machine,
    ),
    ExerciseDefinition(
        "deadlift-barbell",
        "Deadlift (barbell)",
        MuscleGroup.Back,
        Equipment.Barbell,
    ),
    // Chest
    ExerciseDefinition(
        "bench-press-bb",
        "Bench Press (barbell)",
        MuscleGroup.Chest,
        Equipment.Barbell,
    ),
    ExerciseDefinition(
        "incline-bench-press-bb",
        "Incline Bench Press (barbell)",
        MuscleGroup.Chest,
        Equipment.Barbell,
    ),
    ExerciseDefinition(
        "incline-bench-press-db",
        "Incline Bench Press (dumbbell)",
        MuscleGroup.Chest,
        Equipment.Dumbbell,
    ),
    ExerciseDefinition(
        "bench-press-db",
        "Bench Press (dumbbell)",
        MuscleGroup.Chest,
        Equipment.Dumbbell,
    ),
    ExerciseDefinition(
        "chest-press-machine",
        "Chest Press (machine)",
        MuscleGroup.Chest,
        Equipment.Machine,
    ),
    ExerciseDefinition(
        "chest-fly-cable",
        "Chest Fly (cable)",
        MuscleGroup.Chest,
        Equipment.Machine,
    ),
    ExerciseDefinition(
        "chest-fly-peck-deck",
        "Chest Fly (machine)",
        MuscleGroup.Chest,
        Equipment.Machine,
    ),
    ExerciseDefinition(
        "chest-dip",
        "Chest Dip (assisted)",
        MuscleGroup.Chest,
        Equipment.AssistedBodyWeight,
    ),
    // Legs
    ExerciseDefinition("squat-bb", "Squat (barbell)", MuscleGroup.Legs, Equipment.Barbell),
    ExerciseDefinition(
        "rdl-bb",
        "Romanian Deadlift (RDL) (barbell)",
        MuscleGroup.Legs,
        Equipment.Barbell,
    ),
    ExerciseDefinition(
        "split-squat-db",
        "Bulgarian Split Squat (dumbbell)",
        MuscleGroup.Legs,
        Equipment.Dumbbell,
    ),
    ExerciseDefinition("leg-curl", "Leg Curl (machine)", MuscleGroup.Legs, Equipment.Machine),
    ExerciseDefinition("leg-press", "Leg Press (machine)", MuscleGroup.Legs, Equipment.Machine),
    ExerciseDefinition(
        "leg-extension",
        "Leg Extension (machine)",
        MuscleGroup.Legs,
        Equipment.Machine,
    ),
    ExerciseDefinition("squat-machine", "Squat (machine)", MuscleGroup.Legs, Equipment.Machine),
    ExerciseDefinition(
        "standing-calf-raise-machine",
        "Standing Calf Raise (machine)",
        MuscleGroup.Legs,
        Equipment.Machine,
    ),
    // Shoulders
    ExerciseDefinition(
        "lateral-raise-db",
        "Lateral Raise (dumbbell)",
        MuscleGroup.Shoulders,
        Equipment.Dumbbell,
    ),
    ExerciseDefinition(
        "lateral-raise-cable",
        "Lateral Raise (cable)",
        MuscleGroup.Shoulders,
        Equipment.Machine,
    ),
    ExerciseDefinition(
        "upright-row-db",
        "Upright Row (dumbbell)",
        MuscleGroup.Shoulders,
        Equipment.Dumbbell,
    ),
    ExerciseDefinition(
        "lateral-raise-machine",
        "Lateral Raise (machine)",
        MuscleGroup.Shoulders,
        // Wrong: this is a machine; Dumbbell makes increment/default warmup 2 kg / 10 kg.
        Equipment.Dumbbell,
    ),
    ExerciseDefinition("face-pull", "Face Pull (cable)", MuscleGroup.Shoulders, Equipment.Machine),
    ExerciseDefinition(
        "overhead-press-bb",
        "Overhead Press (barbell)",
        MuscleGroup.Shoulders,
        Equipment.Barbell,
    ),
)

internal fun catalogExercise(id: String): ExerciseDefinition = catalogExercisesById.getValue(id)

private val catalogExercisesById = catalogExercises.associateBy { it.id }
