package dev.sanastasov.bybon.account.ui

import dev.sanastasov.bybon.bodyweight.domain.CloudWeight
import dev.sanastasov.bybon.bodyweight.domain.WeightConflict
import dev.sanastasov.bybon.bodyweight.domain.WeightDateClash
import dev.sanastasov.bybon.bodyweight.domain.WeightImportSummary
import dev.sanastasov.bybon.domain.toDisplayDate

data class AccountUiState(
    val environmentName: String,
    val email: String? = null,
    val message: String? = null,
    val confirmSignOut: Boolean = false,
    val signingIn: Boolean = false,
    val backupPending: Boolean = false,
    val lastBackupMillis: Long? = null,
    val conflict: WeightConflict? = null,
    val importSummary: WeightImportSummary? = null,
) {
    val signedIn: Boolean
        get() = email != null

    val conflictChoices: List<AccountAction>
        get() = if (conflict == null) {
            emptyList()
        } else {
            listOf(
                AccountAction.UseTheBackup,
                AccountAction.ReplaceTheBackup,
                AccountAction.KeepThisPhone,
            )
        }
}

fun WeightConflict.detail(): String = when (this) {
    is WeightConflict.DifferentAccount -> "This phone has weights from another account."
    is WeightConflict.OverlappingDates -> dates.detail()
}

private fun List<WeightDateClash>.detail(): String {
    val deleted = count { it.inBackup is CloudWeight.Deleted }
    val single = singleOrNull()
    return when {
        single != null && deleted == 1 ->
            "The backup would delete the weight saved on ${single.date.toDisplayDate()}."

        single != null ->
            "The backup would change the weight saved on ${single.date.toDisplayDate()}."

        deleted == size ->
            "The backup would delete $size weights saved on this phone."

        deleted == 0 ->
            "The backup would change $size weights saved on this phone."

        else ->
            "The backup would change or delete $size weights saved on this phone."
    }
}

sealed interface AccountAction {
    data object OnSignInClicked : AccountAction
    data object OnSignOutClicked : AccountAction
    data object OnSignOutConfirmed : AccountAction
    data object OnSignOutDismissed : AccountAction
    data object UseTheBackup : AccountAction
    data object ReplaceTheBackup : AccountAction
    data object KeepThisPhone : AccountAction
    data object DismissImportSummary : AccountAction
}
