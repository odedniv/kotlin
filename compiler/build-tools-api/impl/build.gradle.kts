plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":build-tools-api"))
    implementation(kotlinStdlib())
    implementation(project(":kotlin-compiler-embeddable"))
    implementation(project(":kotlin-compiler-runner"))
    if (kotlinBuildProperties.isInIdeaSync) {
        compileOnly(project(":kotlin-compiler-runner-unshaded"))
        compileOnly(project(":compiler:cli"))
        compileOnly(project(":compiler:cli-js"))
        compileOnly(project(":kotlin-build-common"))
        compileOnly(project(":daemon-common"))
        compileOnly(project(":kotlin-daemon-client"))
        compileOnly(project(":compiler:incremental-compilation-impl"))
    }
}

publish()

standardPublicJars()