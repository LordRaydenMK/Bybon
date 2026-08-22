package dev.sanastasov.bybon.weight

import dev.sanastasov.bybon.data.DbModule
import dev.sanastasov.bybon.weight.data.WeightRepositoryImpl

interface WeightModule : DbModule {

    val weightRepository: WeightRepository

    companion object {

        fun create(dbModule: DbModule): WeightModule = object : WeightModule, DbModule by dbModule {
            override val weightRepository: WeightRepository
                get() = WeightRepositoryImpl(bybonDb.weightEntryDao())

        }
    }
}