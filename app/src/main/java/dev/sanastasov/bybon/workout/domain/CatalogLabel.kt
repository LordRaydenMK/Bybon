package dev.sanastasov.bybon.workout.domain

import java.util.Locale

fun String.toCatalogLabel(): String = split('_')
    .filter { it.isNotEmpty() }
    .joinToString(" ") { word ->
        word.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.ENGLISH) else char.toString()
        }
    }
