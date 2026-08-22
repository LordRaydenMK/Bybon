package dev.sanastasov.bybon.data.converter

import androidx.room3.ColumnTypeConverter
import java.time.LocalDate

object LocalDateConverter {

    @ColumnTypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @ColumnTypeConverter
    fun toLocalDate(dateStr: String?): LocalDate? = dateStr?.let { LocalDate.parse(it) }
}