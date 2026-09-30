package bybon.repdb

import groovy.json.JsonSlurper
import java.net.HttpURLConnection
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

data class RepdbExercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
)

fun downloadRepdbExercisesJson(url: String = REPDB_EXERCISES_URL): String {
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    connection.connectTimeout = 30_000
    connection.readTimeout = 30_000
    connection.instanceFollowRedirects = true
    connection.setRequestProperty("User-Agent", "Bybon")
    connection.inputStream.bufferedReader().use { return it.readText() }
}

fun parseRepdbCatalog(json: String): List<RepdbExercise> {
    val root = JsonSlurper().parseText(json) as Map<*, *>
    val exercises = root["exercises"] as List<*>
    return exercises.mapNotNull { row ->
        val fields = row as Map<*, *>
        if (!includeExercise(fields)) {
            null
        } else {
            RepdbExercise(
                id = fields["id"] as String,
                name = fields["name_en"] as String,
                muscleGroup = muscleGroup(fields["body_part"] as String),
                equipment = loadClass(fields["equipment"] as? String),
            )
        }
    }
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
    else -> error("Unknown RepDB body part: $bodyPart")
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
