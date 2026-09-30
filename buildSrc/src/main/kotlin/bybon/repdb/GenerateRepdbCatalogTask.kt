package bybon.repdb

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateRepdbCatalogTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val cacheFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val exercises = loadRepdbCatalog(cacheFile.get().asFile, download = ::downloadRepdbExercises)
        val file = outputDirectory.get().asFile.resolve(
            "dev/sanastasov/bybon/workout/domain/RepdbCatalog.kt",
        )
        file.parentFile.mkdirs()
        file.writeText(renderRepdbCatalog(exercises))
    }
}
