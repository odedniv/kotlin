plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(":producer")
            }
        }
    }
}
