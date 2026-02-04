// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    kotlin("multiplatform") version "2.3.0" apply false
}

allprojects {

    group = "de.westnordost"
    version = "1.5"

    repositories {
        mavenCentral()
    }
}