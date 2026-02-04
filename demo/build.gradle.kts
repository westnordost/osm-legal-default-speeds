@file:OptIn(ExperimentalDistributionDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.3.0"
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            distribution {
                outputDirectory = File("$projectDir/distribution")
            }
        }
    }

    sourceSets {
        jsMain {
            dependencies {
                implementation(project(":library"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
            }
        }
        jsTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:2.3.0")
            }
        }
    }
}