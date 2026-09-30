package bybon.repdb

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import groovy.json.JsonSlurper
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

fun repdbExercisesUrl(commit: String): String =
    "https://raw.githubusercontent.com/RepDB/exercise-dataset/$commit/exercises.json"

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

fun downloadRepdbExercisesJson(url: String): String {
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

fun readRepdbExercisesJson(cacheFile: File, url: String): String {
    if (cacheFile.isFile && cacheFile.length() > 0L) {
        return cacheFile.readText()
    }
    val json = downloadRepdbExercisesJson(url)
    cacheFile.parentFile?.mkdirs()
    cacheFile.writeText(json)
    return json
}

private val exerciseDefinition = ClassName("dev.sanastasov.bybon.workout.domain", "ExerciseDefinition")
private val muscleGroupType = ClassName("dev.sanastasov.bybon.workout.domain", "MuscleGroup")
private val equipmentType = ClassName("dev.sanastasov.bybon.workout.domain", "Equipment")

fun repdbCatalogFile(exercises: List<RepdbExercise>): FileSpec {
    val initializer = CodeBlock.builder().add("listOf(")
    exercises.forEach { exercise ->
        initializer.add("\n    %T(", exerciseDefinition)
        initializer.add("\n        %S,", exercise.id)
        initializer.add("\n        %S,", exercise.name)
        initializer.add("\n        %T.%L,", muscleGroupType, exercise.muscleGroup)
        initializer.add("\n        %T.%L,", equipmentType, exercise.equipment)
        initializer.add("\n    ),")
    }
    initializer.add("\n)")
    return FileSpec.builder("dev.sanastasov.bybon.workout.domain", "RepdbCatalog")
        .indent("") // Continuation lines are spaced in the initializer itself.
        .addProperty(
            PropertySpec.builder("repdbCatalogExercises", LIST.parameterizedBy(exerciseDefinition))
                .addModifiers(KModifier.INTERNAL)
                .initializer(initializer.build())
                .build(),
        )
        .build()
}

fun renderRepdbCatalog(exercises: List<RepdbExercise>): String = repdbCatalogFile(exercises).toString()

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
