package dev.sanastasov.bybon.domain

import java.time.LocalDate
import java.time.temporal.WeekFields

val LocalDate.weekOfYear: Int
    get() = get(WeekFields.ISO.weekOfYear())