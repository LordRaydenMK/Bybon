package dev.sanastasov.bybon.domain

import java.time.LocalDate
import org.junit.Test

class LocalDateExtTest {

    @Test
    fun `formats dates as day month year`() {
        assert(LocalDate.of(2026, 8, 10).toDisplayDate() == "10 Aug 2026")
        assert(LocalDate.of(2026, 9, 8).toDisplayDate() == "8 Sep 2026")
    }
}
