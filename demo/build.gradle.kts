plugins {
    kotlin("js")
    kotlin("plugin.serialization") version "1.7.10"
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(project(":library"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.0-RC")
}

kotlin {
    js(LEGACY) {
        binaries.executable()
        browser {
            distribution {
                directory = File("$projectDir/distribution")
            }
        }
    }
}