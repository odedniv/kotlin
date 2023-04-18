plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}

repositories {
    mavenLocal()
    maven("<localRepo>")
}

group = "org.jetbrains.kotlin.tests"
version = "0.1"

kotlin {
    jvm()
    js()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlin.tests:preHmppLibrary:0.1")
            }
        }
    }
}
