package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.workout.data.WorkoutsRepositoryImpl
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository

interface WorkoutModule : DbModule {

    val workoutsRepository: WorkoutsRepository

    companion object {

        // Sessions stay in memory, so one repository outlives Activity recreation.
        private var repository: WorkoutsRepository? = null

        fun create(dbModule: DbModule): WorkoutModule {
            val workouts = repository ?: WorkoutsRepositoryImpl(
                exerciseDao = dbModule.bybonDb.exerciseDao(),
                workoutPlanDao = dbModule.bybonDb.workoutPlanDao(),
            ).also { repository = it }
            return object : WorkoutModule, DbModule by dbModule {
                override val workoutsRepository: WorkoutsRepository = workouts
            }
        }
    }
}
