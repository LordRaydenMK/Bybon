plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("environment") {
            id = "bybon.environment"
            implementationClass = "bybon.environment.EnvironmentConventionPlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlinpoet)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
}
