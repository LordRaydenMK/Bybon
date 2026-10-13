package bybon.repdb

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import groovy.json.JsonSlurper
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
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

const val EXPECTED_REPDB_EXERCISE_COUNT = 382

const val REPDB_DOWNLOAD_TIMEOUT_MILLIS = 30_000

data class RepdbExercise(
    val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val mechanic: String,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val equipmentSlug: String? = null,
    val forceType: String,
    val difficulty: String,
    val tags: List<String> = emptyList(),
    val description: String,
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val images: RepdbImages,
)

sealed interface RepdbImages {
    data class StartAndPeak(
        val start: String,
        val peak: String,
    ) : RepdbImages

    data class Main(
        val main: String,
    ) : RepdbImages
}

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
    url: String,
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
    val slug = equipmentSlug(id, fields["equipment"])
    return RepdbExercise(
        id = id,
        name = name,
        bodyPart = bodyPart(id, bodyPart),
        equipment = loadClass(slug),
        mechanic = mechanic(id, fields["mechanic"]),
        primaryMuscles = muscleSlugs(id, "primary_muscles", fields["primary_muscles"], required = true),
        secondaryMuscles = muscleSlugs(id, "secondary_muscles", fields["secondary_muscles"], required = false),
        equipmentSlug = slug,
        forceType = forceType(id, fields["force_type"]),
        difficulty = difficulty(id, fields["difficulty"]),
        tags = muscleSlugs(id, "tags", fields["tags"], required = false),
        description = englishText(id, "description_en", fields["description_en"]),
        instructions = muscleSlugs(id, "instructions_en", fields["instructions_en"], required = true),
        tips = muscleSlugs(id, "tips_en", fields["tips_en"], required = true),
        images = images(id, fields["images"]),
    )
}

private val exerciseDefinition = ClassName("dev.sanastasov.bybon.workout.domain", "ExerciseDefinition")
private val bodyPartType = ClassName("dev.sanastasov.bybon.workout.domain", "BodyPart")
private val equipmentType = ClassName("dev.sanastasov.bybon.workout.domain", "Equipment")
private val mechanicType = ClassName("dev.sanastasov.bybon.workout.domain", "Mechanic")
private val forceTypeType = ClassName("dev.sanastasov.bybon.workout.domain", "ForceType")
private val difficultyType = ClassName("dev.sanastasov.bybon.workout.domain", "Difficulty")
private val startAndPeakType = ClassName(
    "dev.sanastasov.bybon.workout.domain",
    "ExerciseImages",
    "StartAndPeak",
)
private val mainImageType = ClassName("dev.sanastasov.bybon.workout.domain", "ExerciseImages", "Main")

private const val REPDB_CATALOG_CHUNK_SIZE = 40

fun repdbCatalogFile(exercises: List<RepdbExercise>, commit: String): FileSpec {
    val chunks = exercises.chunked(REPDB_CATALOG_CHUNK_SIZE).ifEmpty { listOf(emptyList()) }
    val builder = FileSpec.builder("dev.sanastasov.bybon.workout.domain", "RepdbCatalog")
        .indent("") // Continuation lines are spaced in the initializer itself.
        .addProperty(imageBaseProperty(commit))
        .addFunction(imageUrlFunction())
    chunks.forEachIndexed { index, chunk ->
        builder.addFunction(catalogChunkFunction(index, chunk))
    }
    return builder.addProperty(catalogProperty(chunks.size)).build()
}

fun renderRepdbCatalog(exercises: List<RepdbExercise>, commit: String = "commit"): String =
    repdbCatalogFile(exercises, commit).toString()

internal fun includeExercise(fields: Map<*, *>): Boolean {
    if (fields["category"] != "strength") return false
    val goals = fields["goals"] as? List<*> ?: emptyList<Any>()
    if ("hypertrophy" !in goals && "strength" !in goals) return false
    val equipment = fields["equipment"] as? String
    if (equipment in excludedEquipment) return false
    val name = fields["name_en"] as? String ?: return false
    return yogaAndPilatesMarkers.none { marker -> marker in name }
}

internal fun bodyPart(id: String, bodyPart: String): String = when (bodyPart) {
    "upper_legs" -> "UpperLegs"
    "lower_legs" -> "LowerLegs"
    "back" -> "Back"
    "upper_arms" -> "UpperArms"
    "lower_arms" -> "LowerArms"
    "chest" -> "Chest"
    "shoulders" -> "Shoulders"
    "core" -> "Core"
    "full_body" -> "FullBody"
    else -> throw RepdbCatalogException("RepDB exercise $id has unknown body part: $bodyPart")
}

internal fun images(id: String, value: Any?): RepdbImages {
    val root = value as? Map<*, *>
        ?: throw RepdbCatalogException("RepDB exercise $id is missing images")
    val flat = root["flat"] as? Map<*, *>
        ?: throw RepdbCatalogException("RepDB exercise $id is missing images")
    val start = imagePath(id, flat["start"])
    val peak = imagePath(id, flat["peak"])
    val main = imagePath(id, flat["main"])
    return when {
        start != null && peak != null && main == null -> RepdbImages.StartAndPeak(start, peak)
        main != null && start == null && peak == null -> RepdbImages.Main(main)
        else -> throw RepdbCatalogException("RepDB exercise $id has invalid images")
    }
}

private fun imagePath(id: String, value: Any?): String? {
    if (value == null) return null
    val path = value as? String
        ?: throw RepdbCatalogException("RepDB exercise $id has invalid images")
    if (path.isBlank()) throw RepdbCatalogException("RepDB exercise $id has invalid images")
    return path
}

internal fun imageUrl(commit: String, path: String): String =
    "https://raw.githubusercontent.com/RepDB/exercise-dataset/$commit/$path"

private fun imageBaseProperty(commit: String): PropertySpec =
    PropertySpec.builder("REPDB_IMAGE_BASE", STRING, KModifier.PRIVATE, KModifier.CONST)
        .initializer("%S", imageUrl(commit, ""))
        .build()

private fun imageUrlFunction(): FunSpec =
    FunSpec.builder("repdbImageUrl")
        .addModifiers(KModifier.PRIVATE)
        .addParameter("path", STRING)
        .returns(STRING)
        .addStatement("return REPDB_IMAGE_BASE + path")
        .build()

private fun catalogChunkFunction(index: Int, chunk: List<RepdbExercise>): FunSpec =
    FunSpec.builder("repdbCatalogChunk$index")
        .addModifiers(KModifier.PRIVATE)
        .returns(LIST.parameterizedBy(exerciseDefinition))
        .addCode("return %L", exerciseList(chunk))
        .build()

private fun catalogProperty(chunkCount: Int): PropertySpec {
    val initializer = CodeBlock.builder()
    repeat(chunkCount) { index ->
        if (index > 0) initializer.add(" +\n    ")
        initializer.add("repdbCatalogChunk%L()", index)
    }
    return PropertySpec.builder("repdbCatalogExercises", LIST.parameterizedBy(exerciseDefinition))
        .addModifiers(KModifier.INTERNAL)
        .initializer(initializer.build())
        .build()
}

private fun exerciseList(exercises: List<RepdbExercise>): CodeBlock {
    val initializer = CodeBlock.builder().add("listOf(")
    exercises.forEach { exercise ->
        initializer.add("\n    %T(", exerciseDefinition)
        initializer.add("\n        %S,", exercise.id)
        initializer.add("\n        %S,", exercise.name)
        initializer.add("\n        %T.%L,", bodyPartType, exercise.bodyPart)
        initializer.add("\n        %T.%L,", equipmentType, exercise.equipment)
        initializer.add("\n        %T.%L,", mechanicType, exercise.mechanic)
        initializer.add("\n        %L,", muscleList(exercise.primaryMuscles))
        initializer.add("\n        %L,", muscleList(exercise.secondaryMuscles))
        initializer.add("\n        %L,", equipmentSlugLiteral(exercise.equipmentSlug))
        initializer.add("\n        %T.%L,", forceTypeType, exercise.forceType)
        initializer.add("\n        %T.%L,", difficultyType, exercise.difficulty)
        initializer.add("\n        %L,", muscleList(exercise.tags))
        initializer.add("\n        %S,", exercise.description)
        initializer.add("\n        %L,", muscleList(exercise.instructions))
        initializer.add("\n        %L,", muscleList(exercise.tips))
        initializer.add("\n        %L,", imagesLiteral(exercise.images))
        initializer.add("\n    ),")
    }
    initializer.add("\n)")
    return initializer.build()
}

private fun imagesLiteral(images: RepdbImages): CodeBlock = when (images) {
    is RepdbImages.StartAndPeak -> CodeBlock.builder()
        .add("%T(\n", startAndPeakType)
        .add("            repdbImageUrl(%S),\n", images.start)
        .add("            repdbImageUrl(%S),\n", images.peak)
        .add("        )")
        .build()
    is RepdbImages.Main -> CodeBlock.builder()
        .add("%T(\n", mainImageType)
        .add("            repdbImageUrl(%S),\n", images.main)
        .add("        )")
        .build()
}

internal fun englishText(id: String, field: String, value: Any?): String {
    val text = value as? String
    if (text.isNullOrBlank()) {
        throw RepdbCatalogException("RepDB exercise $id is missing $field")
    }
    return text
}

internal fun muscleSlugs(id: String, field: String, value: Any?, required: Boolean): List<String> {
    if (value == null) {
        if (required) throw RepdbCatalogException("RepDB exercise $id is missing $field")
        return emptyList()
    }
    val slugs = value as? List<*>
        ?: throw RepdbCatalogException("RepDB exercise $id has invalid $field")
    return slugs.map { slug ->
        slug as? String ?: throw RepdbCatalogException("RepDB exercise $id has invalid $field")
    }
}

private fun muscleList(muscles: List<String>): CodeBlock {
    if (muscles.isEmpty()) return CodeBlock.of("emptyList()")
    val block = CodeBlock.builder().add("listOf(")
    muscles.forEach { muscle -> block.add("%S, ", muscle) }
    return block.add(")").build()
}

internal fun mechanic(id: String, value: Any?): String = when (value) {
    "compound" -> "Compound"
    "isolation" -> "Isolation"
    null -> throw RepdbCatalogException("RepDB exercise $id is missing mechanic")
    else -> throw RepdbCatalogException("RepDB exercise $id has unknown mechanic: $value")
}

internal fun equipmentSlug(id: String, value: Any?): String? = when (value) {
    null -> null
    is String -> value.takeIf { it.isNotBlank() }
        ?: throw RepdbCatalogException("RepDB exercise $id has invalid equipment")
    else -> throw RepdbCatalogException("RepDB exercise $id has invalid equipment")
}

private fun equipmentSlugLiteral(slug: String?): CodeBlock =
    if (slug == null) CodeBlock.of("null") else CodeBlock.of("%S", slug)

internal fun forceType(id: String, value: Any?): String = when (value) {
    "push" -> "Push"
    "pull" -> "Pull"
    "static" -> "Static"
    "dynamic" -> "Dynamic"
    null -> throw RepdbCatalogException("RepDB exercise $id is missing force_type")
    else -> throw RepdbCatalogException("RepDB exercise $id has unknown force type: $value")
}

internal fun difficulty(id: String, value: Any?): String = when (value) {
    "beginner" -> "Beginner"
    "intermediate" -> "Intermediate"
    "advanced" -> "Advanced"
    null -> throw RepdbCatalogException("RepDB exercise $id is missing difficulty")
    else -> throw RepdbCatalogException("RepDB exercise $id has unknown difficulty: $value")
}

internal fun loadClass(equipment: String?): String = when (equipment) {
    null -> "Bodyweight"
    "barbell", "trap_bar" -> "Barbell"
    "dumbbell" -> "Dumbbell"
    "assisted_pullup_machine", "dip_machine" -> "AssistedBodyWeight"
    else -> "Machine"
}
