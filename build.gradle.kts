// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    kotlin("multiplatform") version "1.8.21" apply false
}

allprojects {

    group = "de.westnordost"
    version = "1.3"

    repositories {
        mavenCentral()
    }
}