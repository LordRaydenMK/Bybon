package dev.sanastasov.bybon.account

import android.content.Context

fun Context.googleWebClientId(): String {
    val id = resources.getIdentifier("default_web_client_id", "string", packageName)
    require(id != 0) { "Missing default_web_client_id" }
    return getString(id)
}
