package dev.sanastasov.bybon.data.converter

import androidx.room3.ColumnTypeConverter
import java.time.LocalDateTime

object LocalDateTimeConverter {

    @ColumnTypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()

    @ColumnTypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)
}
