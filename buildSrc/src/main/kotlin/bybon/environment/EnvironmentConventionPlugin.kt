package bybon.environment

import bybon.firebase.FirebaseEnv
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class EnvironmentConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val envOverride = providers.gradleProperty("env").orNull
        FirebaseEnv.validateOverride(envOverride)
        registerGoogleServicesCopyTasks(envOverride)
        wireGoogleServicesJson()
    }

    private fun Project.registerGoogleServicesCopyTasks(envOverride: String?) {
        listOf("debug", "release").forEach { buildType ->
            val env = FirebaseEnv.resolve(envOverride, buildType)
            val json = FirebaseEnv.googleServicesFile(rootDir, env)
            tasks.register<Copy>(FirebaseEnv.copyGoogleServicesTaskName(buildType)) {
                description = "Copy firebase/$env/google-services.json for the $buildType variant"
                from(json)
                into(layout.buildDirectory.dir("generated/google-services/$buildType"))
                rename { "google-services.json" }
            }
        }
    }

    private fun Project.wireGoogleServicesJson() {
        pluginManager.withPlugin("com.google.gms.google-services") {
            afterEvaluate {
                // GoogleServicesTask is created lazily and lives in the plugin classloader.
                val taskType = googleServicesTaskType()
                tasks.withType(taskType).configureEach {
                    val buildType = FirebaseEnv.buildTypeFromGoogleServicesTaskName(name)
                    val copy = tasks.named<Copy>(FirebaseEnv.copyGoogleServicesTaskName(buildType))
                    dependsOn(copy)
                    googleServicesJsonFiles().set(
                        copy.map { listOf(it.destinationDir.resolve("google-services.json")) },
                    )
                }
            }
        }
    }

    private fun Project.googleServicesTaskType(): Class<out Task> {
        val plugin = plugins.findPlugin("com.google.gms.google-services")
            ?: error("com.google.gms.google-services is not applied")
        return Class.forName(
            "com.google.gms.googleservices.GoogleServicesTask",
            true,
            plugin.javaClass.classLoader,
        ).asSubclass(Task::class.java)
    }

    private fun Task.googleServicesJsonFiles(): Property<Collection<File>> {
        @Suppress("UNCHECKED_CAST")
        return javaClass.getMethod("getGoogleServicesJsonFiles").invoke(this)
            as Property<Collection<File>>
    }
}
