package bybon.repdb

import groovy.json.JsonSlurper
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI

const val REPDB_COMMIT = "9ed9357f09c7566ea0256c57ebd6374ebb8b575e"

const val REPDB_EXERCISES_URL =
    "https://raw.githubusercontent.com/RepDB/exercise-dataset/$REPDB_COMMIT/exercises.json"

private val excludedEquipment = setOf("kettlebell", "loop_band", "resistance_band")

private val yogaAndPilatesMarkers = listOf(
    "Pilates",
    "Pose",
    "Warrior",
    "Downward Dog",
    "Upward Dog",
    "Crescent Lunge",
    "Shoulderstand",
)

const val EXPECTED_REPDB_EXERCISE_COUNT = 382

const val REPDB_DOWNLOAD_TIMEOUT_MILLIS = 30_000

data class RepdbExercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
)

class RepdbCatalogException(message: String) : RuntimeException(message)

sealed interface RepdbDownload {
    data class Ok(val body: String) : RepdbDownload

    data class HttpStatus(val statusCode: Int) : RepdbDownload

    data object TimedOut : RepdbDownload

    data class Failed(val detail: String) : RepdbDownload
}

private data class RepdbJsonSource(
    val json: String,
    val fromCache: Boolean,
)

fun downloadRepdbExercises(
    url: String = REPDB_EXERCISES_URL,
    timeoutMillis: Int = REPDB_DOWNLOAD_TIMEOUT_MILLIS,
): RepdbDownload {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    connection.connectTimeout = timeoutMillis
    connection.readTimeout = timeoutMillis
    connection.instanceFollowRedirects = true
    connection.setRequestProperty("User-Agent", "Bybon")
    return try {
        val statusCode = connection.responseCode
        if (statusCode != HttpURLConnection.HTTP_OK) {
            RepdbDownload.HttpStatus(statusCode)
        } else {
            RepdbDownload.Ok(connection.inputStream.bufferedReader().use { it.readText() })
        }
    } catch (_: SocketTimeoutException) {
        RepdbDownload.TimedOut
    } catch (error: IOException) {
        RepdbDownload.Failed(error.message ?: error.javaClass.simpleName)
    } finally {
        connection.disconnect()
    }
}

fun loadRepdbCatalog(
    cacheFile: File,
    expectedCount: Int = EXPECTED_REPDB_EXERCISE_COUNT,
    download: () -> RepdbDownload,
): List<RepdbExercise> {
    val source = readRepdbExercisesJson(cacheFile, download)
    val exercises = parseRepdbCatalog(source.json, expectedCount)
    if (!source.fromCache) {
        writeRepdbExercisesCache(cacheFile, source.json)
    }
    return exercises
}

fun parseRepdbCatalog(json: String, expectedCount: Int? = null): List<RepdbExercise> {
    val root = parseRoot(json)
    val schema = (root["schema_version"] as? Number)?.toInt()
    if (schema != 3) {
        throw RepdbCatalogException("RepDB catalog schema is $schema, expected 3")
    }
    val rows = root["exercises"] as? List<*>
        ?: throw RepdbCatalogException("RepDB catalog is missing exercises")
    val included = rows.mapNotNull { row -> toExercise(row) }
    if (expectedCount != null && included.size != expectedCount) {
        throw RepdbCatalogException(
            "RepDB catalog filter kept ${included.size} exercises, expected $expectedCount",
        )
    }
    return included
}

private fun readRepdbExercisesJson(cacheFile: File, download: () -> RepdbDownload): RepdbJsonSource {
    if (cacheFile.isFile && cacheFile.length() > 0L) {
        return RepdbJsonSource(cacheFile.readText(), fromCache = true)
    }
    val json = when (val result = download()) {
        is RepdbDownload.Ok -> result.body
        is RepdbDownload.HttpStatus ->
            throw RepdbCatalogException("RepDB download failed: HTTP ${result.statusCode}")
        RepdbDownload.TimedOut ->
            throw RepdbCatalogException("RepDB download failed: timed out")
        is RepdbDownload.Failed ->
            throw RepdbCatalogException("RepDB download failed: ${result.detail}")
    }
    return RepdbJsonSource(json, fromCache = false)
}

private fun writeRepdbExercisesCache(cacheFile: File, json: String) {
    cacheFile.parentFile?.mkdirs()
    cacheFile.writeText(json)
}

private fun parseRoot(json: String): Map<*, *> {
    val parsed = try {
        JsonSlurper().parseText(json)
    } catch (_: Exception) {
        throw RepdbCatalogException("RepDB catalog is not valid JSON")
    }
    return parsed as? Map<*, *> ?: throw RepdbCatalogException("RepDB catalog is not valid JSON")
}

private fun toExercise(row: Any?): RepdbExercise? {
    val fields = row as? Map<*, *> ?: throw RepdbCatalogException("RepDB exercise is missing id")
    val id = fields["id"] as? String
    if (id.isNullOrBlank()) {
        throw RepdbCatalogException("RepDB exercise is missing id")
    }
    val name = fields["name_en"] as? String
    if (name.isNullOrBlank()) {
        throw RepdbCatalogException("RepDB exercise $id is missing name_en")
    }
    if (!includeExercise(fields)) return null
    val bodyPart = fields["body_part"] as? String
        ?: throw RepdbCatalogException("RepDB exercise $id is missing body_part")
    return RepdbExercise(
        id = id,
        name = name,
        muscleGroup = muscleGroup(bodyPart),
        equipment = loadClass(fields["equipment"] as? String),
    )
}

fun renderRepdbCatalog(exercises: List<RepdbExercise>): String = buildString {
    appendLine("package dev.sanastasov.bybon.workout.domain")
    appendLine()
    appendLine("internal val repdbCatalogExercises: List<ExerciseDefinition> = listOf(")
    exercises.forEach { exercise ->
        appendLine("    ExerciseDefinition(")
        appendLine("        ${kotlinString(exercise.id)},")
        appendLine("        ${kotlinString(exercise.name)},")
        appendLine("        MuscleGroup.${exercise.muscleGroup},")
        appendLine("        Equipment.${exercise.equipment},")
        appendLine("    ),")
    }
    appendLine(")")
    appendLine()
}

internal fun includeExercise(fields: Map<*, *>): Boolean {
    if (fields["category"] != "strength") return false
    val goals = fields["goals"] as? List<*> ?: emptyList<Any>()
    if ("hypertrophy" !in goals && "strength" !in goals) return false
    val equipment = fields["equipment"] as? String
    if (equipment in excludedEquipment) return false
    val name = fields["name_en"] as? String ?: return false
    return yogaAndPilatesMarkers.none { marker -> marker in name }
}

internal fun muscleGroup(bodyPart: String): String = when (bodyPart) {
    "upper_legs", "lower_legs" -> "Legs"
    "back" -> "Back"
    "upper_arms", "lower_arms" -> "Arms"
    "chest" -> "Chest"
    "shoulders" -> "Shoulders"
    "core" -> "Core"
    "full_body" -> "FullBody"
    else -> throw RepdbCatalogException("RepDB catalog has unknown body part: $bodyPart")
}

internal fun loadClass(equipment: String?): String = when (equipment) {
    null -> "Bodyweight"
    "barbell", "trap_bar" -> "Barbell"
    "dumbbell" -> "Dumbbell"
    "assisted_pullup_machine", "dip_machine" -> "AssistedBodyWeight"
    else -> "Machine"
}

private fun kotlinString(value: String): String = buildString {
    append('"')
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '$' -> append("\\$")
            else -> append(character)
        }
    }
    append('"')
}
