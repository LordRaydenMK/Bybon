package dev.sanastasov.bybon.workout

import android.app.AlarmManager
import android.content.Context
import androidx.core.content.getSystemService
import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.workout.data.WorkoutsRepositoryImpl
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository

interface WorkoutModule : DbModule {

    val workoutsRepository: WorkoutsRepository
    val alarmManager: AlarmManager
    val restPromoteScheduler: RestPromoteScheduler

    companion object {

        private var repository: WorkoutsRepository? = null

        fun create(dbModule: DbModule, appContext: Context): WorkoutModule {
            val db = dbModule.bybonDb
            val workouts = repository ?: WorkoutsRepositoryImpl(
                exerciseDao = db.exerciseDao(),
                workoutPlanDao = db.workoutPlanDao(),
                workoutSessionDao = db.workoutSessionDao(),
            ).also { repository = it }
            val alarms = appContext.getSystemService<AlarmManager>()
                ?: error("Failed to get AlarmManager")
            val restAlarms = WorkoutSessionRestAlarm(appContext, alarms)
            return object : WorkoutModule, DbModule by dbModule {
                override val workoutsRepository: WorkoutsRepository = workouts
                override val alarmManager: AlarmManager = alarms
                override val restPromoteScheduler: RestPromoteScheduler = restAlarms
            }
        }
    }
}
