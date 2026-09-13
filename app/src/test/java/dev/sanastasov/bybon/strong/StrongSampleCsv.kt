package dev.sanastasov.bybon.strong

fun readStrongBackupSample(classLoader: ClassLoader?): String {
    return checkNotNull(classLoader?.getResourceAsStream("strong-backup-sample.csv")) {
        "Missing test resource strong-backup-sample.csv"
    }.bufferedReader().use { it.readText() }
}
