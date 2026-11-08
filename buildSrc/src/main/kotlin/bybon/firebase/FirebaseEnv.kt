package bybon.firebase

import java.io.File

object FirebaseEnv {
    fun validateOverride(override: String?) {
        if (override != null && override != "stg" && override != "prd") {
            error("Unknown env '$override'. Use -Penv=stg or -Penv=prd.")
        }
    }

    fun resolve(override: String?, buildType: String): String {
        if (override != null) return override
        return if (buildType == "debug") "stg" else "prd"
    }

    fun googleServicesFile(rootDir: File, env: String): File {
        val file = File(rootDir, "firebase/$env/google-services.json")
        require(file.isFile) {
            "Missing ${file.path} for env=$env."
        }
        return file
    }

    fun copyGoogleServicesTaskName(buildType: String): String =
        "copy${buildType.replaceFirstChar { it.uppercase() }}GoogleServices"

    fun buildTypeFromGoogleServicesTaskName(taskName: String): String = when {
        "Release" in taskName -> "release"
        "Debug" in taskName -> "debug"
        else -> error("Cannot infer build type from Google Services task '$taskName'")
    }
}
