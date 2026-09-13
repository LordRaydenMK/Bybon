package dev.sanastasov.bybon.strong

import com.jsoizo.kotlincsv.CsvDialect
import com.jsoizo.kotlincsv.csvReader
import com.jsoizo.kotlincsv.reader.withHeader

object StrongCsvParser {

    private val requiredColumns = listOf(
        "Workout #",
        "Date",
        "Workout Name",
        "Duration (sec)",
        "Exercise Name",
        "Set Order"
    )

    fun parse(csv: String): List<StrongCsvRow> {
        val reader = csvReader {
            dialect = CsvDialect(delimiter = ';')
        }

        val records = reader.readAll(csv)
        require(records.isNotEmpty()) { "Strong CSV is empty" }
        val header = records.first()
        require(requiredColumns.all { it in header }) { "Not a Strong CSV export" }

        return records
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
                    workoutNotes = record["Workout Notes"].blankToNull()
                )
            }
            .toList()
    }

    private fun String?.blankToNull(): String? = this?.takeIf { it.isNotBlank() }
}
