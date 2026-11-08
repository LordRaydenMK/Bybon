package dev.sanastasov.bybon.main

import dev.sanastasov.bybon.account.AccountModule
import dev.sanastasov.bybon.bodyweight.BodyWeightModule
import dev.sanastasov.bybon.bodyweight.data.FirestoreWeightCloud
import dev.sanastasov.bybon.bodyweight.sync.WeightBackup
import dev.sanastasov.bybon.bodyweight.sync.WeightBackupController
import dev.sanastasov.bybon.bodyweight.sync.WeightSyncScheduler
import dev.sanastasov.bybon.data.BybonDatabase
import dev.sanastasov.bybon.workout.WorkoutModule

interface MainModule :
    BodyWeightModule,
    WorkoutModule,
    AccountModule {

    val appScope: AppScope

    val weightBackup: WeightBackup

    val weightSyncScheduler: WeightSyncScheduler

    companion object {
        private val scope = AppScope()

        fun create(
            bodyWeightModule: BodyWeightModule,
            workoutModule: WorkoutModule,
            accountModule: AccountModule,
            scheduler: WeightSyncScheduler,
        ): MainModule = object :
            MainModule,
            BodyWeightModule by bodyWeightModule,
            WorkoutModule by workoutModule,
            AccountModule by accountModule {
            override val appScope: AppScope = scope

            override val weightSyncScheduler: WeightSyncScheduler = scheduler

            override val weightBackup: WeightBackup = WeightBackupController(
                bodyWeightRepository,
                FirestoreWeightCloud(firebaseEnvName),
            )

            override val bybonDb: BybonDatabase
                get() = bodyWeightModule.bybonDb
        }
    }
}
