/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.appendText

@EnabledOnOs(OS.MAC)
@DisplayName("Tests for K/N with cocoapods")
@NativeGradlePluginTests
class NativeLibraryDslWithCocoapodsIT : KGPBaseTest() {

    @DisplayName("K/N project registers shared tasks")
    @GradleTest
    fun shouldCheckGradleRegisteredTasks(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":shared:tasks", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksRegistered(
                    ":shared:generateMylibStaticLibraryLinuxX64Podspec",
                    ":shared:generateMyslibSharedLibraryLinuxX64Podspec",
                    ":shared:generateMyframeFrameworkIosArm64Podspec",
                    ":shared:generateMyfatframeFatFrameworkPodspec",
                    ":shared:generateSharedXCFrameworkPodspec",
                    ":lib:generateGrooframeFrameworkIosArm64Podspec",
                    ":lib:generateGrooxcframeXCFrameworkPodspec",
                    ":shared:generateMyframewihtoutpodspecFrameworkIosArm64Podspec",
                    ":lib:generateGrooxcframewithoutpodspecXCFrameworkPodspec",
                )
            }
        }
    }

    @DisplayName("K/N project generates podspec when assembling static lib for Linux X64")
    @GradleTest
    fun shouldGeneratePodspecWithStaticLibForLinuxX64(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":shared:assembleMylibStaticLibraryLinuxX64") {
                assertTasksExecuted(":shared:generateMylibStaticLibraryLinuxX64Podspec")
                assertFilesContentEqual(
                    projectPath.resolve("podspecs/mylib.podspec"),
                    projectPath.resolve("./shared/build/out/static/mylib.podspec"),
                )
            }
        }
    }

    @DisplayName("K/N project generates podspec when assembling shared lib for Linux X64")
    @GradleTest
    fun shouldGeneratePodspecWithSharedLibForLinuxX64(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":shared:assembleMyslibSharedLibraryLinuxX64") {
                assertTasksExecuted(":shared:generateMyslibSharedLibraryLinuxX64Podspec")
                assertFilesContentEqual(
                    projectPath.resolve("podspecs/myslib.podspec"),
                    projectPath.resolve("./shared/build/out/dynamic/myslib.podspec")
                )
            }
        }
    }

    @DisplayName("K/N project does not generate podspec with empty withPodspec for Linux X64")
    @GradleTest
    fun shouldNotGeneratePodspecWithoutPodspecForLinuxX64(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":shared:assembleMyslibwithoutpodspecSharedLibraryLinuxX64") {
                assertTasksSkipped(":shared:generateMyslibwithoutpodspecSharedLibraryLinuxX64Podspec")
                assertOutputContains("Skipping task ':shared:generateMyslibwithoutpodspecSharedLibraryLinuxX64Podspec' because there are no podspec attributes defined")
            }
        }
    }

    @DisplayName("K/N project generates podspec when assembling framework for Ios Arm 64")
    @GradleTest
    fun shouldGeneratePodspecWithFrameworkIosArm64(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":shared:assembleMyframeFrameworkIosArm64") {
                assertTasksExecuted(":shared:generateMyframeFrameworkIosArm64Podspec")
                assertFilesContentEqual(
                    projectPath.resolve("podspecs/myframe.podspec"),
                    projectPath.resolve("./shared/build/out/framework/myframe.podspec")
                )
            }
        }
    }

    @DisplayName("K/N project does not generate podspec with empty withPodspec for Ios Arm 64")
    @GradleTest
    fun shouldNotGeneratePodspecWithoutPodspecForIosArm64(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":shared:assembleMyframewihtoutpodspecFrameworkIosArm64") {
                assertTasksSkipped(":shared:generateMyframewihtoutpodspecFrameworkIosArm64Podspec")
                assertOutputContains("Skipping task ':shared:generateMyframewihtoutpodspecFrameworkIosArm64Podspec' because there are no podspec attributes defined")
            }
        }
    }

    @DisplayName("K/N project generates podspec when assembling fat framework")
    @GradleTest
    fun shouldGeneratePodspecWithFatFramework(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":shared:assembleMyfatframeFatFramework") {
                assertTasksExecuted(":shared:generateMyfatframeFatFrameworkPodspec")
                assertFilesContentEqual(
                    projectPath.resolve("podspecs/myfatframe.podspec"),
                    projectPath.resolve("./shared/build/out/fatframework/myfatframe.podspec")
                )
            }
        }
    }

    @DisplayName("K/N project generates podspec when assembling xcframework")
    @GradleTest
    fun shouldGeneratePodspecWithXCFramework(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":shared:assembleSharedXCFramework") {
                assertTasksExecuted(":shared:generateSharedXCFrameworkPodspec")
                assertFilesContentEqual(
                    projectPath.resolve("podspecs/shared.podspec"),
                    projectPath.resolve("./shared/build/out/xcframework/shared.podspec")
                )
            }
        }
    }

    @DisplayName("K/N project generates podspec when assembling xcframework from groovy")
    @GradleTest
    fun shouldGeneratePodspecWithXCFrameworkFromGroovy(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":lib:assembleGrooframeFrameworkIosArm64") {
                assertTasksExecuted(":lib:generateGrooframeFrameworkIosArm64Podspec")
                assertFilesContentEqual(
                    projectPath.resolve("podspecs/grooframe.podspec"),
                    projectPath.resolve("./lib/build/out/framework/grooframe.podspec")
                )
            }
        }
    }

    @DisplayName("K/N project does not generate podspec with empty withPodspec from groovy")
    @GradleTest
    fun shouldNotGeneratePodspecWithEmptyPodspecFromGrooby(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":lib:assembleGrooxcframeXCFramework") {
                assertTasksSkipped(":lib:generateGrooxcframeXCFrameworkPodspec")
            }
        }
    }

    @DisplayName("K/N project does not generate podspec from groovy when there is no withPodspec")
    @GradleTest
    fun shouldNotGeneratePodspecFromGroovyWithoutPodspec(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            build(":lib:assembleGrooxcframewithoutpodspecXCFramework") {
                assertTasksSkipped(":lib:generateGrooxcframewithoutpodspecXCFrameworkPodspec")
            }
        }
    }

    @DisplayName("K/N project generates podspecs when several frameworks with the same name")
    @GradleTest
    fun shouldGeneratePodspecsWhenSeveralFrameworksWithTheSameName(gradleVersion: GradleVersion) {
        buildNewKnLibraryDslCocoapodsProjectWithTasks(gradleVersion) {
            projectPath.resolve("shared/build.gradle.kts").appendText(
                """
                kotlinArtifacts {
                    Native.Library {
                         target = linuxX64
                         
                         withPodspec {}
                    }
                }
            """.trimIndent()
            )

            build(":shared:assembleSharedXCFramework")
        }
    }

    private fun buildNewKnLibraryDslCocoapodsProjectWithTasks(
        gradleVersion: GradleVersion,
        buildBlock: TestProject.() -> Unit
    ) {
        nativeProject("new-kn-library-dsl-cocoapods", gradleVersion).buildBlock()
    }
}