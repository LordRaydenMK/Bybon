package dev.sanastasov.bybon.workout.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "repdb_catalog")
data class RepdbCatalogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "repdb_commit") val commit: String,
)
