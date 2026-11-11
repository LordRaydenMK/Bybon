package dev.sanastasov.bybon.bodyweight

import dev.sanastasov.bybon.bodyweight.data.BodyWeightRepositoryImpl
import dev.sanastasov.bybon.bodyweight.domain.BodyWeightRepository
import dev.sanastasov.bybon.bodyweight.sync.WeightCloud
import dev.sanastasov.bybon.data.DbModule

interface BodyWeightModule : DbModule {

    val bodyWeightRepository: BodyWeightRepository

    companion object {

        fun create(
            dbModule: DbModule,
            databaseName: String,
            cloud: WeightCloud,
        ): BodyWeightModule = object : BodyWeightModule, DbModule by dbModule {
            override val bodyWeightRepository: BodyWeightRepository = BodyWeightRepositoryImpl(
                dbModule.bybonDb.weightEntryDao(),
                dbModule.bybonDb.dietPhaseDao(),
                dbModule.bybonDb.weightSyncDao(),
                cloud,
                databaseName,
            )
        }
    }
}
