package dev.sanastasov.bybon.workout.domain

import okhttp3.HttpUrl

sealed interface ExerciseImages {
    data class StartAndPeak(
        val start: HttpUrl,
        val peak: HttpUrl,
    ) : ExerciseImages

    data class Main(
        val main: HttpUrl,
    ) : ExerciseImages
}

val ExerciseImages.libraryUrl: HttpUrl
    get() = when (this) {
        is ExerciseImages.StartAndPeak -> peak
        is ExerciseImages.Main -> main
    }
