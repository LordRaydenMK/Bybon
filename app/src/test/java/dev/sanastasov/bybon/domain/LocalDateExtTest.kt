package dev.sanastasov.bybon.domain

import java.time.LocalDate
import org.junit.Test

class LocalDateExtTest {

    @Test
    fun `formats dates as day month year`() {
        assert(LocalDate.of(2026, 8, 10).toDisplayDate() == "10 Aug 2026")
        assert(LocalDate.of(2026, 9, 8).toDisplayDate() == "8 Sep 2026")
        assert(LocalDate.of(2026, 8, 10).toShortDisplayDate() == "10 Aug")
        assert(LocalDate.of(2026, 10, 1).toShortDisplayDate() == "1 Oct")
    }

    @Test
    fun `relative dates use days up to 30 then months`() {
        val today = LocalDate.of(2026, 9, 9)
        assert(
            LocalDate.of(2026, 9, 9).toDisplayDateDotRelative(today) == "9 Sep 2026 · today",
        )
        assert(
            LocalDate.of(2026, 9, 8).toDisplayDateDotRelative(today) == "8 Sep 2026 · 1 day ago",
        )
        assert(
            LocalDate.of(2026, 8, 10).toDisplayDateDotRelative(today) ==
                "10 Aug 2026 · 30 days ago",
        )
        assert(
            LocalDate.of(2026, 8, 9).toDisplayDateDotRelative(today) ==
                "9 Aug 2026 · 1 month ago",
        )
        assert(
            LocalDate.of(2026, 7, 9).toDisplayDateDotRelative(today) ==
                "9 Jul 2026 · 2 months ago",
        )
        assert(
            LocalDate.of(2026, 11, 6).toDisplayDateDotRelative(LocalDate.of(2026, 11, 10)) ==
                "6 Nov 2026 · 4 days ago",
        )
    }

    @Test
    fun `iso week starts on Monday`() {
        val wednesday = LocalDate.of(2026, 9, 9)
        assert(wednesday.isoWeekStart() == LocalDate.of(2026, 9, 7))
        assert(LocalDate.of(2026, 9, 7).isoWeekStart() == LocalDate.of(2026, 9, 7))
        assert(LocalDate.of(2026, 9, 13).isoWeekStart() == LocalDate.of(2026, 9, 7))
    }
}
