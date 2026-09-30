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
        val url = repdbExercisesUrl(commit)
        val cacheFile = cacheDirectory.file("$commit.json").get().asFile
        if (cacheFile.isFile && cacheFile.length() > 0L) {
            logger.lifecycle("RepDB exercises cache hit: {}", cacheFile)
        }
        val exercises = loadRepdbCatalog(cacheFile) {
            logger.lifecycle("Downloading RepDB exercises from {}", url)
            downloadRepdbExercises(url).also(::logDownloadResult)
        }
        logger.lifecycle("Wrote RepDB catalog with {} exercises", exercises.size)
        repdbCatalogFile(exercises).writeTo(outputDirectory.get().asFile)
    }

    private fun logDownloadResult(result: RepdbDownload) {
        when (result) {
            is RepdbDownload.Ok ->
                logger.lifecycle("Downloaded RepDB exercises: {} bytes", result.body.length)
            is RepdbDownload.HttpStatus ->
                logger.error("RepDB download failed: HTTP {}", result.statusCode)
            RepdbDownload.TimedOut ->
                logger.error("RepDB download failed: timed out")
            is RepdbDownload.Failed ->
                logger.error("RepDB download failed: {}", result.detail)
        }
    }
}
