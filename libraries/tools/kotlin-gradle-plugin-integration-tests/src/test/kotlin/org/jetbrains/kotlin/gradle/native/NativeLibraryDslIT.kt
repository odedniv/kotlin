/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.appendText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("Tests for K/N library dsl builds")
@NativeGradlePluginTests
class NativeLibraryDslIT : KGPBaseTest() {

    @DisplayName("Checks registered gradle tasks")
    @GradleTest
    fun shouldSharedAndLibRegisteredTasks(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            build(":shared:tasks", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted()
                assertTasksRegistered(
                    ":shared:assembleMyfatframeFatFramework",
                    ":shared:assembleMyframeFrameworkIosArm64",
                    ":shared:assembleMylibSharedLibraryLinuxX64",
                    ":shared:assembleMyslibSharedLibraryLinuxX64",
                    ":shared:assembleSharedXCFramework"
                )
                assertTasksNotRegistered(
                    ":shared:assembleMyslibReleaseSharedLibraryLinuxX64"
                )
            }
            build(":lib:tasks", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksRegistered(
                    ":lib:assembleGroofatframeFatFramework",
                    ":lib:assembleGrooframeFrameworkIosArm64",
                    ":lib:assembleGroolibSharedLibraryIosX64",
                    ":lib:assembleLibXCFramework"
                )
            }
        }
    }

    @DisplayName("Checks link shared libraries from two gradle modules")
    @GradleTest
    fun shouldLinkSharedLibrariesFromTwoModules(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            build(":shared:assembleMyslibDebugSharedLibraryLinuxX64") {
                assertTasksExecuted(
                    ":lib:compileKotlinLinuxX64",
                    ":shared:compileKotlinLinuxX64",
                    ":shared:assembleMyslibDebugSharedLibraryLinuxX64"
                )
                assertFileExists(projectPath.resolve("./shared/build/out/dynamic/linux_x64/debug/libmyslib.so"))
                assertFileExists(projectPath.resolve("./shared/build/out/dynamic/linux_x64/debug/libmyslib_api.h"))
            }
        }
    }

    @DisplayName("Checks link shared library from single gradle module")
    @GradleTest
    fun shouldLinkSharedLibrariesFromSingleModule(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            build(":shared:assembleMylibDebugSharedLibraryLinuxX64", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted(
                    ":shared:compileKotlinLinuxX64",
                    ":shared:assembleMylibDebugSharedLibraryLinuxX64"
                )
                assertTasksNotExecuted(
                    ":lib:compileKotlinLinuxX64"
                )
                assertTasksCommandLineArguments(":shared:assembleMylibDebugSharedLibraryLinuxX64") {
                    assertFalse(it.contains("-Xfoo=bar"))
                    assertFalse(it.contains("-Xbaz=qux"))
                    assertTrue(it.contains("-Xmen=pool"))
                }
                assertFileExists(projectPath.resolve("./shared/build/out/dynamic/linux_x64/debug/libmylib.so"))
                assertFileExists(projectPath.resolve("./shared/build/out/dynamic/linux_x64/debug/libmylib_api.h"))
            }
        }
    }

    @DisplayName("Checks link shared library from single gradle module with additional link args")
    @GradleTest
    fun shouldLinkSharedLibrariesFromSingleModuleWithAdditionalLinkArgs(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            gradleProperties.appendText("\nkotlin.native.linkArgs=-Xfoo=bar -Xbaz=qux")
            build(":shared:assembleMylibDebugSharedLibraryLinuxX64", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksExecuted(
                    ":shared:compileKotlinLinuxX64",
                    ":shared:assembleMylibDebugSharedLibraryLinuxX64"
                )
                assertTasksNotExecuted(
                    ":lib:compileKotlinLinuxX64"
                )
                assertTasksCommandLineArguments(":shared:assembleMylibDebugSharedLibraryLinuxX64") {
                    Assertions.assertTrue(it.contains("-Xfoo=bar"))
                    Assertions.assertTrue(it.contains("-Xbaz=qux"))
                    Assertions.assertTrue(it.contains("-Xmen=pool"))
                }
                assertFileExists(projectPath.resolve("./shared/build/out/dynamic/linux_x64/debug/libmylib.so"))
                assertFileExists(projectPath.resolve("./shared/build/out/dynamic/linux_x64/debug/libmylib_api.h"))
            }
        }
    }

    @EnabledOnOs(OS.MAC)
    @DisplayName("Checks link release XCFramework from two gradle modules")
    @GradleTest
    fun shouldLinkXCFrameworkFromTwoModules(gradleVersion: GradleVersion) {
        nativeProject("new-kn-library-dsl", gradleVersion) {
            makeSnapshotTo("/Users/Dmitrii.Krasnov/Projects/kotlin/snapshot-test-dir")
            build(":shared:assembleSharedReleaseXCFramework") {
                assertTasksExecuted(
                    ":shared:compileKotlinIosX64",
                    ":shared:compileKotlinIosArm64",
                    ":shared:compileKotlinIosSimulatorArm64",
                    ":lib:compileKotlinIosX64",
                    ":lib:compileKotlinIosArm64",
                    ":lib:compileKotlinIosSimulatorArm64",
                    ":shared:assembleSharedReleaseXCFramework"
                )
                assertTasksNotExecuted(
                    ":shared:assembleSharedDebugXCFramework"
                )
                assertFileExists(projectPath.resolve("./shared/build/out/xcframework/release/shared.xcframework"), false)
            }
        }
    }

}