plugins {
    kotlin("multiplatform")
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("org.jetbrains.dokka") version "2.1.0"
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }

    applyDefaultHierarchyTemplate()
}

dokka {
    moduleName.set("OSM Legal Default Speeds")
    dokkaSourceSets {
        configureEach {
            sourceLink {
                remoteUrl("https://github.com/westnordost/osm-legal-default-speeds/tree/v${project.version}/")
                localDirectory = rootDir
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), rootProject.name, version.toString())

    pom {
        name = "osm-legal-default-speeds"
        description = "Infer default legal speed limits from OpenStreetMap tags"
        url = "https://github.com/westnordost/osm-legal-default-speeds"
        licenses {
            license {
                name = "BSD 3-Clause License"
                url = "https://raw.githubusercontent.com/westnordost/osm-legal-default-speeds/master/LICENSE.txt"
            }
        }
        issueManagement {
            system = "GitHub"
            url = "https://github.com/westnordost/osm-legal-default-speeds/issues"
        }
        scm {
            connection = "https://github.com/westnordost/osm-legal-default-speeds.git"
            url = "https://github.com/westnordost/osm-legal-default-speeds"
        }
        developers {
            developer {
                id = "westnordost"
                name = "Tobias Zwick"
                email = "osm@westnordost.de"
            }
        }
    }
}
