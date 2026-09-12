package dev.sanastasov.bybon.strong

import org.junit.Assert.assertEquals
import org.junit.Test

class StrongCsvRowTest {

    @Test
    fun `reads strong backup sample into dto rows`() {
        val rows = StrongCsvParser.readSample(javaClass.classLoader)

        println("Strong CSV entries: ${rows.size}")
        assertEquals(2053, rows.size)
    }

    @Test
    fun `parses strong backup sample into workout sessions`() {
        val sessions = StrongCsvParser.readSample(javaClass.classLoader).toWorkoutSessions()

        println("Workout sessions: ${sessions.size}")
        assertEquals(52, sessions.size)
    }
}
