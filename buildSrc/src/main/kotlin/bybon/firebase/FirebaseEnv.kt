package bybon.firebase

import groovy.json.JsonSlurper
import java.io.File

object FirebaseEnv {
    fun validateOverride(override: String?) {
        if (override != null && override != "stg" && override != "prd") {
            error("Unknown env '$override'. Use -Penv=stg or -Penv=prd.")
        }
    }

    fun resolve(override: String?, buildType: String, stgConfigExists: Boolean): String {
        if (override != null) return override
        return if (buildType == "debug" && stgConfigExists) "stg" else "prd"
    }

    fun googleServicesFile(rootDir: File, env: String): File {
        val file = File(rootDir, "firebase/$env/google-services.json")
        require(file.isFile) {
            "Missing ${file.path} for env=$env."
        }
        return file
    }

    @Suppress("UNCHECKED_CAST")
    fun webClientId(googleServicesJson: File): String {
        val parsed = JsonSlurper().parse(googleServicesJson) as Map<*, *>
        val clients = parsed["client"] as? List<*> ?: return ""
        for (client in clients) {
            val oauthClients = (client as? Map<*, *>)?.get("oauth_client") as? List<*> ?: continue
            for (oauthClient in oauthClients) {
                val oauth = oauthClient as? Map<*, *> ?: continue
                if ((oauth["client_type"] as? Number)?.toInt() == 3) {
                    return oauth["client_id"] as? String ?: ""
                }
            }
        }
        return ""
    }
}
