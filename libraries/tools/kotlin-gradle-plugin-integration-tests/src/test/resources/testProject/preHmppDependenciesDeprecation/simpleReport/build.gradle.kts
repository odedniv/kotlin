plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    maven("<localRepo>")
}

kotlin {
    jvm()
    linuxX64()

    sourceSets.getByName("commonMain").dependencies {
        implementation("org.jetbrains.kotlin.tests:preHmppLibrary:0.1")
    }
}
