package dev.sanastasov.bybon.workout.ui.history

import android.net.Uri
import org.mockito.Mockito.mock

class FakeContentResolverReader(
    private val csv: String = "",
) : ContentResolverReader {

    override fun read(uri: Uri): String = csv
}

internal fun dummyUri(): Uri = mock(Uri::class.java)
