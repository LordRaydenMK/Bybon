package dev.sanastasov.bybon.workout.ui.history

import android.net.DummyUri
import android.net.Uri

class FakeContentResolverReader(
    private val csv: String = "",
) : ContentResolverReader {

    override fun read(uri: Uri): String = csv
}

internal fun dummyUri(): Uri = DummyUri()
