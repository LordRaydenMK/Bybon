package bybon.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FirebaseEnvTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `override wins for every build type`() {
        assertEquals("prd", FirebaseEnv.resolve("prd", "debug"))
        assertEquals("stg", FirebaseEnv.resolve("stg", "release"))
    }

    @Test
    fun `debug defaults to stg`() {
        assertEquals("stg", FirebaseEnv.resolve(null, "debug"))
    }

    @Test
    fun `release defaults to prd`() {
        assertEquals("prd", FirebaseEnv.resolve(null, "release"))
    }

    @Test
    fun `unknown override fails`() {
        assertThrows(IllegalStateException::class.java) {
            FirebaseEnv.validateOverride("dev")
        }
    }

    @Test
    fun `missing google services file fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            FirebaseEnv.googleServicesFile(tmp.root, "stg")
        }
    }
}
