package bybon.firebase

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
}
