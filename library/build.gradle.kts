plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.7.10"
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(BOTH) {
        browser()
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("osm-legal-default-speeds")
                description.set("Infer default legal speed limits from OpenStreetMap tags")
                url.set("https://github.com/westnordost/osm-legal-default-speeds")
                licenses {
                    license {
                        name.set("BSD 3-Clause License")
                        url.set("https://raw.githubusercontent.com/westnordost/osm-legal-default-speeds/master/LICENSE.txt")
                    }
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/westnordost/osm-legal-default-speeds/issues")
                }
                scm {
                    connection.set("https://github.com/westnordost/osm-legal-default-speeds.git")
                    url.set("https://github.com/westnordost/osm-legal-default-speeds")
                }
                developers {
                    developer {
                        id.set("westnordost")
                        name.set("Tobias Zwick")
                        email.set("osm@westnordost.de")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "oss"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                val ossrhUsername: String by project
                val ossrhPassword: String by project
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

signing {
    sign(publishing.publications)
}
