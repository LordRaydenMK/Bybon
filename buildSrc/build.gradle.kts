plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinpoet)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
}
