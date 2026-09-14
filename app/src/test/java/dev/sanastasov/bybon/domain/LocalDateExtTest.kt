package dev.sanastasov.bybon.domain

import java.time.LocalDate
import org.junit.Test

class LocalDateExtTest {

    @Test
    fun `formats dates as day month year`() {
        assert(LocalDate.of(2026, 8, 10).toDisplayDate() == "10 Aug 2026")
        assert(LocalDate.of(2026, 9, 8).toDisplayDate() == "8 Sep 2026")
    }

    @Test
    fun `iso week starts on Monday`() {
        val wednesday = LocalDate.of(2026, 9, 9)
        assert(wednesday.isoWeekStart() == LocalDate.of(2026, 9, 7))
        assert(LocalDate.of(2026, 9, 7).isoWeekStart() == LocalDate.of(2026, 9, 7))
        assert(LocalDate.of(2026, 9, 13).isoWeekStart() == LocalDate.of(2026, 9, 7))
    }
}
