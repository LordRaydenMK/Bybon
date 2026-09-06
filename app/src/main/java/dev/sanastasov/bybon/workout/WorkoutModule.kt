package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.workout.data.WorkoutPlansRepositoryImpl
import dev.sanastasov.bybon.workout.domain.WorkoutPlansRepository

interface WorkoutModule {

    val workoutPlansRepository: WorkoutPlansRepository

    companion object {

        fun create(): WorkoutModule = object : WorkoutModule {

            override val workoutPlansRepository: WorkoutPlansRepository =
                WorkoutPlansRepositoryImpl()
        }
    }
}