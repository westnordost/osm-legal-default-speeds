// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    kotlin("multiplatform") version "1.7.10" apply false
}

allprojects {

    group = "de.westnordost"
    version = "1.1"

    repositories {
        mavenCentral()
    }
}