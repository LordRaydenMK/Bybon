package dev.sanastasov.bybon.workout

import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.workout.data.WorkoutsRepositoryImpl
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository

interface WorkoutModule : DbModule {

    val workoutsRepository: WorkoutsRepository

    companion object {

        private var repository: WorkoutsRepository? = null

        fun create(dbModule: DbModule): WorkoutModule {
            val db = dbModule.bybonDb
            val workouts = repository ?: WorkoutsRepositoryImpl(
                exerciseDao = db.exerciseDao(),
                workoutPlanDao = db.workoutPlanDao(),
                workoutSessionDao = db.workoutSessionDao(),
            ).also { repository = it }
            return object : WorkoutModule, DbModule by dbModule {
                override val workoutsRepository: WorkoutsRepository = workouts
            }
        }
    }
}
