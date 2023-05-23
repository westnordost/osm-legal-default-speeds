plugins {
    kotlin("js")
    kotlin("plugin.serialization") version "1.8.21"
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(project(":library"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            distribution {
                directory = File("$projectDir/distribution")
            }
        }
    }
}