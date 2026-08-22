package dev.sanastasov.bybon

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {

    @Serializable
    data object MainScreen : Screen

    @Serializable
    data object WeightEntryScreen : Screen
}