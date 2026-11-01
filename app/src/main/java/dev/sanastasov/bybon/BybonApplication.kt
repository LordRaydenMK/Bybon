package dev.sanastasov.bybon

import android.app.Application
import android.content.Context
import dev.sanastasov.bybon.bodyweight.BodyWeightModule
import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.main.MainModule
import dev.sanastasov.bybon.workout.WorkoutModule

class BybonApplication : Application() {

    val mainModule: MainModule by lazy(LazyThreadSafetyMode.NONE) {
        val dbModule = DbModule.create(this)
        MainModule.create(
            BodyWeightModule.create(dbModule),
            WorkoutModule.create(dbModule, this),
        )
    }
}

val Context.mainModule: MainModule
    get() = (applicationContext as BybonApplication).mainModule
