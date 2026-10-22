package dev.sanastasov.bybon.workout.domain

import org.junit.Test

class CatalogLabelTest {

    @Test
    fun `title-cases underscore slugs`() {
        assert("pectoralis_major".toCatalogLabel() == "Pectoralis Major")
        assert("no_axial_load".toCatalogLabel() == "No Axial Load")
        assert("Push".toCatalogLabel() == "Push")
    }
}
