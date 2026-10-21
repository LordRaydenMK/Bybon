package dev.sanastasov.bybon.main

import dev.sanastasov.bybon.bodyweight.BodyWeightModule
import dev.sanastasov.bybon.data.BybonDatabase
import dev.sanastasov.bybon.workout.WorkoutModule

interface MainModule :
    BodyWeightModule,
    WorkoutModule {

    val appScope: AppScope

    companion object {
        private val scope = AppScope()

        fun create(bodyWeightModule: BodyWeightModule, workoutModule: WorkoutModule): MainModule =
            object :
                MainModule,
                BodyWeightModule by bodyWeightModule,
                WorkoutModule by workoutModule {
                override val appScope: AppScope = scope

                override val bybonDb: BybonDatabase
                    get() = bodyWeightModule.bybonDb
            }
    }
}
