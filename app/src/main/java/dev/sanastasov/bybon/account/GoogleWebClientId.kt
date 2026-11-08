package dev.sanastasov.bybon.account

import android.content.Context

fun Context.googleWebClientId(): String {
    val id = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (id == 0) "" else getString(id)
}
