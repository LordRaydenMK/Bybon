package bybon.repdb

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateRepdbCatalogTask : DefaultTask() {
    @get:Input
    abstract val commit: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val cacheDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val commit = commit.get()
        val cacheFile = cacheDirectory.file("$commit.json").get().asFile
        val exercises = loadRepdbCatalog(cacheFile) {
            downloadRepdbExercises(repdbExercisesUrl(commit))
        }
        repdbCatalogFile(exercises).writeTo(outputDirectory.get().asFile)
    }
}
