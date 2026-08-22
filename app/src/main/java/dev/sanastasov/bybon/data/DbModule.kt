package dev.sanastasov.bybon.data

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver

interface DbModule {
    val bybonDb: BybonDatabase

    companion object {
        fun create(appContext: Application): DbModule {
            val db = Room.databaseBuilder<BybonDatabase>(appContext, "bybon_db")
                .setDriver(AndroidSQLiteDriver())
                .build()

            return object : DbModule {
                override val bybonDb: BybonDatabase
                    get() = db
            }
        }
    }
}