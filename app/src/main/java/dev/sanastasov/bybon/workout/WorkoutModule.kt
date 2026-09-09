package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.workout.data.WorkoutsRepositoryImpl
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository

interface WorkoutModule {

    val workoutsRepository: WorkoutsRepository

    companion object {

        fun create(): WorkoutModule = object : WorkoutModule {

            override val workoutsRepository: WorkoutsRepository = WorkoutsRepositoryImpl()
        }
    }
}