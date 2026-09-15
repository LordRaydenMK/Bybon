import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.powerassert.gradle.PowerAssertCompilationFilter
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.power.assert)
    alias(libs.plugins.androidx.room3)
    alias(libs.plugins.detekt)
}

android {
    namespace = "dev.sanastasov.bybon"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.sanastasov.bybon"
        minSdk = 31
        targetSdk = 37

        versionCode = System.getenv("VERSION_CODE")?.toInt() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "0.1-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("androidTest") {
            assets.directories.add("schemas")
        }
    }

    val keystorePropertiesFile = rootProject.file("keystore.properties")

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                val keystoreProperties = Properties().apply {
                    keystorePropertiesFile.inputStream().use(::load)
                }

                storeFile = rootProject.file("keystore.jks")
                storePassword = keystoreProperties["bybon_ks_pass"] as String
                keyAlias = keystoreProperties["bybon_key_alias"] as String
                keyPassword = keystoreProperties["bybon_key_pass"] as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    testFixtures {
        enable = true
    }
}

base {
    archivesName.set("Bybon-${providers.environmentVariable("VERSION_NAME").getOrElse("0.1-dev")}")
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    basePath.set(rootProject.projectDir)
}

tasks.withType<Detekt>().configureEach {
    jvmTarget.set("21")
    exclude("**/build/**")
    exclude("**/generated/**")
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    functions = listOf(
        "kotlin.assert",
        "kotlin.test.assertTrue",
        "kotlin.test.assertEquals",
        "kotlin.test.assertNull"
    )
    compilationFilter = PowerAssertCompilationFilter {
        it.name.contains("debug", ignoreCase = true) &&
                !it.name.contains("testFixtures", ignoreCase = true)
    }
}

dependencies {
    detektPlugins(libs.detekt.rules.ktlint.wrapper)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)
    implementation(libs.androidx.room.common)

    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)

    implementation(libs.kotlinx.serialization.core)

    implementation(libs.retained)
    implementation(libs.compose.charts)

    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesImplementation(libs.androidx.compose.runtime)
    testFixturesImplementation(libs.kotlinx.coroutines.core)

    implementation(libs.kotlin.csv)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.room3.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}