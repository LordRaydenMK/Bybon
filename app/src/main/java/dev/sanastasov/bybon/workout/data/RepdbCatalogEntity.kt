package dev.sanastasov.bybon.workout.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

internal const val REPDB_CATALOG_ROW_ID = 1

@Entity(tableName = "repdb_catalog")
data class RepdbCatalogEntity(
    @PrimaryKey val id: Int = REPDB_CATALOG_ROW_ID,
    @ColumnInfo(name = "repdb_commit") val commit: String,
)
