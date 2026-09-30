package bybon.repdb

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateRepdbCatalogTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val exercises = parseRepdbCatalog(downloadRepdbExercisesJson())
        val file = outputDirectory.get().asFile.resolve(
            "dev/sanastasov/bybon/workout/domain/RepdbCatalog.kt",
        )
        file.parentFile.mkdirs()
        file.writeText(renderRepdbCatalog(exercises))
    }
}
