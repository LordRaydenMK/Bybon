package dev.sanastasov.bybon

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import dev.sanastasov.bybon.bodyweight.BodyWeightModule
import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.main.MainModule
import dev.sanastasov.bybon.workout.WorkoutForegroundDriver
import dev.sanastasov.bybon.workout.WorkoutForegroundService
import dev.sanastasov.bybon.workout.WorkoutModule

class BybonApplication : Application() {
    lateinit var dbModule: DbModule
        private set
    lateinit var workoutModule: WorkoutModule
        private set
    lateinit var mainModule: MainModule
        private set

    override fun onCreate() {
        super.onCreate()
        val db = DbModule.create(this)
        val workout = WorkoutModule.create(db)
        dbModule = db
        workoutModule = workout
        mainModule = MainModule.create(
            BodyWeightModule.create(db),
            workout,
        )
        WorkoutForegroundDriver(
            repository = workout.workoutsRepository,
            scope = mainModule.appScope,
            startService = {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, WorkoutForegroundService::class.java),
                )
            },
            stopService = {
                stopService(Intent(this, WorkoutForegroundService::class.java))
            },
        ).start()
    }
}
