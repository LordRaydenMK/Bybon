package dev.sanastasov.bybon

import android.app.Application
import android.content.Context
import dev.sanastasov.bybon.account.AccountModule
import dev.sanastasov.bybon.bodyweight.BodyWeightModule
import dev.sanastasov.bybon.bodyweight.sync.WorkManagerWeightSyncScheduler
import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.main.MainModule
import dev.sanastasov.bybon.workout.WorkoutModule

class BybonApplication : Application() {

    val mainModule: MainModule by lazy(LazyThreadSafetyMode.NONE) {
        val scheduler = WorkManagerWeightSyncScheduler(this)
        val dbModule = DbModule.create(this)
        val module = MainModule.create(
            BodyWeightModule.create(dbModule) { scheduler.enqueueImmediate() },
            WorkoutModule.create(dbModule, this),
            AccountModule.create(),
            scheduler,
        )
        scheduler.ensureDaily()
        module
    }

    override fun onCreate() {
        super.onCreate()
        mainModule
    }
}

val Context.mainModule: MainModule
    get() = (applicationContext as BybonApplication).mainModule
