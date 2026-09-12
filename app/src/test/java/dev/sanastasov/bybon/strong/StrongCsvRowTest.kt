package dev.sanastasov.bybon.strong

import com.jsoizo.kotlincsv.CsvDialect
import com.jsoizo.kotlincsv.csvReader
import com.jsoizo.kotlincsv.reader.withHeader
import org.junit.Assert.assertEquals
import org.junit.Test

class StrongCsvRowTest {

    @Test
    fun `reads strong backup sample into dto rows`() {
        val rows = readStrongCsvSample()

        println("Strong CSV entries: ${rows.size}")
        assertEquals(2053, rows.size)
    }

    private fun readStrongCsvSample(): List<StrongCsvRow> {
        val csv = checkNotNull(
            javaClass.classLoader?.getResourceAsStream("strong-backup-sample.csv")
        ) { "Missing test resource strong-backup-sample.csv" }
            .bufferedReader()
            .use { it.readText() }

        val reader = csvReader {
            dialect = CsvDialect(delimiter = ';')
        }

        return reader.readAll(csv)
            .asSequence()
            .withHeader()
            .map { record ->
                StrongCsvRow(
                    workoutNumber = record.getValue("Workout #").toInt(),
                    date = record.getValue("Date"),
                    workoutName = record.getValue("Workout Name"),
                    durationSec = record.getValue("Duration (sec)").toInt(),
                    exerciseName = record.getValue("Exercise Name"),
                    setOrder = record.getValue("Set Order"),
                    weightKg = record["Weight (kg)"].blankToNull()?.toDouble(),
                    reps = record["Reps"].blankToNull()?.toInt(),
                    rpe = record["RPE"].blankToNull()?.toDouble(),
                    distanceMeters = record["Distance (meters)"].blankToNull()?.toDouble(),
                    seconds = record["Seconds"].blankToNull()?.toDouble(),
                    notes = record["Notes"].blankToNull(),
                    workoutNotes = record["Workout Notes"].blankToNull(),
                )
            }
            .toList()
    }

    private fun String?.blankToNull(): String? =
        this?.takeIf { it.isNotBlank() }
}
