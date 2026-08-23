package dev.sanastasov.bybon.bodyweight

import dev.sanastasov.bybon.bodyweight.data.BodyWeightRepositoryImpl
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.data.DbModule

interface BodyWeightModule : DbModule {

    val bodyWeightRepository: BodyWeightRepository

    companion object {

        fun create(dbModule: DbModule): BodyWeightModule =
            object : BodyWeightModule, DbModule by dbModule {
                override val bodyWeightRepository: BodyWeightRepository
                    get() = BodyWeightRepositoryImpl(bybonDb.weightEntryDao())

            }
    }
}