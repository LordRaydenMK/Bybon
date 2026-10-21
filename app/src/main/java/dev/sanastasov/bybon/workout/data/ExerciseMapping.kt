package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.AppJson
import dev.sanastasov.bybon.workout.domain.BodyPart
import dev.sanastasov.bybon.workout.domain.Difficulty
import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseImages
import dev.sanastasov.bybon.workout.domain.ForceType
import dev.sanastasov.bybon.workout.domain.Mechanic
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import okhttp3.HttpUrl.Companion.toHttpUrl

private val stringListSerializer = ListSerializer(String.serializer())

internal fun ExerciseDefinition.toEntity(
    source: String,
    isArchived: Boolean = false,
): ExerciseEntity {
    val (imageStart, imagePeak, imageMain) = when (val images = images) {
        is ExerciseImages.StartAndPeak -> Triple(
            images.start.toString(),
            images.peak.toString(),
            null,
        )

        is ExerciseImages.Main -> Triple(null, null, images.main.toString())

        null -> Triple(null, null, null)
    }
    return ExerciseEntity(
        id = id,
        name = name,
        bodyPart = bodyPart?.name,
        equipment = equipment?.name,
        mechanic = mechanic?.name,
        primaryMuscles = primaryMuscles.toJsonArray(),
        secondaryMuscles = secondaryMuscles.toJsonArray(),
        equipmentSlug = equipmentSlug,
        forceType = forceType?.name,
        difficulty = difficulty?.name,
        tags = tags.toJsonArray(),
        description = description,
        instructions = instructions?.toJsonArray(),
        tips = tips?.toJsonArray(),
        imageStart = imageStart,
        imagePeak = imagePeak,
        imageMain = imageMain,
        source = source,
        isArchived = isArchived,
    )
}

internal fun ExerciseEntity.toDefinition(): ExerciseDefinition = ExerciseDefinition(
    id = id,
    name = name,
    bodyPart = bodyPart?.let(BodyPart::valueOf),
    equipment = equipment?.let(Equipment::valueOf),
    mechanic = mechanic?.let(Mechanic::valueOf),
    primaryMuscles = primaryMuscles.toStringList(),
    secondaryMuscles = secondaryMuscles.toStringList(),
    equipmentSlug = equipmentSlug,
    forceType = forceType?.let(ForceType::valueOf),
    difficulty = difficulty?.let(Difficulty::valueOf),
    tags = tags.toStringList(),
    description = description,
    instructions = instructions?.toStringList(),
    tips = tips?.toStringList(),
    images = toImages(),
)

private fun ExerciseEntity.toImages(): ExerciseImages? = when {
    imageStart != null && imagePeak != null -> ExerciseImages.StartAndPeak(
        start = imageStart.toHttpUrl(),
        peak = imagePeak.toHttpUrl(),
    )

    imageMain != null -> ExerciseImages.Main(imageMain.toHttpUrl())

    else -> null
}

private fun List<String>.toJsonArray(): String =
    AppJson.json.encodeToString(stringListSerializer, this)

private fun String.toStringList(): List<String> =
    AppJson.json.decodeFromString(stringListSerializer, this)
