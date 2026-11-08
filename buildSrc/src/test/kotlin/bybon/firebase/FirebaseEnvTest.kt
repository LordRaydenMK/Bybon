package bybon.firebase

import java.io.File
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
        assertEquals("prd", FirebaseEnv.resolve("prd", "debug", stgConfigExists = true))
        assertEquals("stg", FirebaseEnv.resolve("stg", "release", stgConfigExists = false))
    }

    @Test
    fun `debug defaults to stg when the stg config exists`() {
        assertEquals("stg", FirebaseEnv.resolve(null, "debug", stgConfigExists = true))
    }

    @Test
    fun `debug defaults to prd until the stg config exists`() {
        assertEquals("prd", FirebaseEnv.resolve(null, "debug", stgConfigExists = false))
    }

    @Test
    fun `release defaults to prd`() {
        assertEquals("prd", FirebaseEnv.resolve(null, "release", stgConfigExists = true))
        assertEquals("prd", FirebaseEnv.resolve(null, "release", stgConfigExists = false))
    }

    @Test
    fun `unknown override fails`() {
        assertThrows(IllegalStateException::class.java) {
            FirebaseEnv.validateOverride("dev")
        }
    }

    @Test
    fun `reads the web oauth client id`() {
        val file = tmp.newFile("google-services.json")
        file.writeText(
            """
            {
              "client": [
                {
                  "oauth_client": [
                    { "client_id": "android.apps.googleusercontent.com", "client_type": 1 },
                    { "client_id": "web.apps.googleusercontent.com", "client_type": 3 }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("web.apps.googleusercontent.com", FirebaseEnv.webClientId(file))
    }

    @Test
    fun `empty oauth client list has no web id`() {
        val file = tmp.newFile("empty.json")
        file.writeText(
            """
            {
              "client": [
                { "oauth_client": [] }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("", FirebaseEnv.webClientId(file))
    }

    @Test
    fun `missing google services file fails`() {
        assertThrows(IllegalArgumentException::class.java) {
            FirebaseEnv.googleServicesFile(tmp.root, "stg")
        }
    }
}
