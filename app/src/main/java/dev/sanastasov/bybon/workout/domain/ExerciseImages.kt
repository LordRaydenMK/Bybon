package dev.sanastasov.bybon.workout.domain

sealed interface ExerciseImages {
    data class StartAndPeak(
        val start: String,
        val peak: String,
    ) : ExerciseImages

    data class Main(
        val main: String,
    ) : ExerciseImages
}

val ExerciseImages.libraryUrl: String
    get() = when (this) {
        is ExerciseImages.StartAndPeak -> peak
        is ExerciseImages.Main -> main
    }
