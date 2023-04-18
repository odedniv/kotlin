package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.DEPRECATED_PRE_HMPP_LIBRARIES_DETECTED_MESSAGE
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

@MppGradlePluginTests
class PreHmppDependenciesDeprecationIT : KGPBaseTest() {

    @GradleTest
    fun testSimpleReport(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)
        checkDiagnostics(gradleVersion, "simpleReport", expectReportForDependency = "preHmppLibrary")
    }

    @GradleTest
    fun noReportFromTransitiveDependencies(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)
        publishLibrary("hmppLibraryWithPreHmppInDependencies", gradleVersion, tempDir)
        checkDiagnostics(gradleVersion, "reportFromTransitiveDependencies")
    }

    @GradleTest
    fun noReportWhenSuppressed(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)
        checkDiagnostics(gradleVersion, "simpleReport") {
            gradleProperties.writeText("kotlin.mpp.allow.legacy.dependencies=true")
        }
    }

    @GradleTest
    fun testNoWarningsOnPopularDependencies(gradleVersion: GradleVersion) {
        checkDiagnostics(gradleVersion, "noWarningsOnPopularDependencies")
    }

    @GradleTest
    fun testNoWarningsOnProjectDependencies(gradleVersion: GradleVersion) {
        checkDiagnostics(gradleVersion, "noWarningsOnProjectDependencies")
    }

    @GradleTest
    fun testNoWarningsInPlatformSpecificSourceSets(gradleVersion: GradleVersion) {
        checkDiagnostics(gradleVersion, "noWarningsInPlatformSpecificSourceSets")
    }

    @GradleTest
    fun testNoWarningsInPreHmppProjects(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        publishLibrary("preHmppLibrary", gradleVersion, tempDir)
        checkDiagnostics(gradleVersion, "simpleReport") {
            gradleProperties.writeText("kotlin.internal.mpp.hierarchicalStructureByDefault=false")
        }
    }

    private fun checkDiagnostics(
        gradleVersion: GradleVersion,
        projectName: String,
        expectReportForDependency: String? = null,
        preBuildAction: TestProject.() -> Unit = {}
    ) {
        project("preHmppDependenciesDeprecation/$projectName", gradleVersion) {
            preBuildAction()
            build("dependencies", enableGradleDebug = true) {
                if (expectReportForDependency != null) {
                    assertOutputContainsExactlyTimes(
                        DEPRECATED_PRE_HMPP_LIBRARIES_DETECTED_MESSAGE.replace("{0}", ".*$expectReportForDependency.*").toRegex()
                    )
                } else {
                    assertOutputDoesNotContain(
                        DEPRECATED_PRE_HMPP_LIBRARIES_DETECTED_MESSAGE.replace("{0}", ".*").toRegex()
                    )
                }
            }
        }
    }

    private fun publishLibrary(name: String, gradleVersion: GradleVersion, tempDir: Path) {
        project("preHmppDependenciesDeprecation/$name", gradleVersion, localRepoDir = tempDir.resolve("repo")) {
            build("publish")
        }
    }
}
