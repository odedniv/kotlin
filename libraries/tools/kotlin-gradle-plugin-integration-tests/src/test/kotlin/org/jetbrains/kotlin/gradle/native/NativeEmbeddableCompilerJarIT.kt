/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("Tests for K/N builds with embeddable compiler jar")
@NativeGradlePluginTests
internal class NativeEmbeddableCompilerJarIT : KGPBaseTest() {

    private fun String.isRegularJar() = this.endsWith("/kotlin-native.jar")
    private fun String.isEmbeddableJar() = this.endsWith("/kotlin-native-compiler-embeddable.jar")

    private fun List<String>.includesRegularJar() = any { it.isRegularJar() }
    private fun List<String>.includesEmbeddableJar() = any { it.isEmbeddableJar() }

    private val String.withPrefix get() = "native-binaries/$this"

    @DisplayName("K/N with default config shouldn't contain kotlin-native.jar and should contain kotlin-native-compiler-embeddable.jar")
    @GradleTest
    fun shouldNotUseRegularJarInDefaultConfig(gradleVersion: GradleVersion) {
        nativeProject("executables".withPrefix, gradleVersion) {
            build(":linkDebugExecutableHost", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assertFalse(it.includesRegularJar(), "Actual classpath is: $it")
                    assertTrue(it.includesEmbeddableJar(), "Actual classpath is: $it")
                }
            }
        }
    }

    @DisplayName("K/N with embeddable compiler flag turned off config should contain kotlin-native.jar and shouldn't contain kotlin-native-compiler-embeddable.jar")
    @GradleTest
    fun shouldUseRegularJarWithoutUseEmbeddableCompilerJar(gradleVersion: GradleVersion) {
        nativeProject("executables".withPrefix, gradleVersion) {
            build(
                ":linkDebugExecutableHost",
                "-Pkotlin.native.useEmbeddableCompilerJar=false",
                buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)
            ) {
                assertTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assertTrue(it.includesRegularJar(), "Actual classpath is: $it")
                    assertFalse(it.includesEmbeddableJar(), "Actual classpath is: $it")
                }
            }
        }
    }

    @DisplayName("K/N with embeddable compiler flag turned on config shouldn't contain kotlin-native.jar and should contain kotlin-native-compiler-embeddable.jar")
    @GradleTest
    fun shouldUseRegularJarWithUseEmbeddableCompilerJar(gradleVersion: GradleVersion) {
        nativeProject("executables".withPrefix, gradleVersion) {
            build(
                ":linkDebugExecutableHost",
                "-Pkotlin.native.useEmbeddableCompilerJar=true",
                buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)
            ) {
                assertTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assertFalse(it.includesRegularJar(), "Actual classpath is: $it")
                    assertTrue(it.includesEmbeddableJar(), "Actual classpath is: $it")
                }
            }
        }
    }

    @DisplayName("K/N project's builds with switching embeddable compiler flag turned from on to off")
    @GradleTest
    fun testSwitch(gradleVersion: GradleVersion) {
        nativeProject("executables".withPrefix, gradleVersion) {
            build(":linkDebugExecutableHost", buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)) {
                assertTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assertFalse(it.includesRegularJar(), "Actual classpath is: $it")
                    assertTrue(it.includesEmbeddableJar(), "Actual classpath is: $it")
                }
            }

            build(":linkDebugExecutableHost") {
                assertTasksUpToDate(":linkDebugExecutableHost", ":compileKotlinHost")
            }

            build(
                ":linkDebugExecutableHost",
                "-Pkotlin.native.useEmbeddableCompilerJar=false",
                buildOptions = buildOptions.copy(logLevel = LogLevel.DEBUG)
            ) {
                assertTasksExecuted(":linkDebugExecutableHost", ":compileKotlinHost")
                assertTasksClasspath(":linkDebugExecutableHost", ":compileKotlinHost") {
                    assertTrue(it.includesRegularJar(), "Actual classpath is: $it")
                    assertFalse(it.includesEmbeddableJar(), "Actual classpath is: $it")
                }
            }
        }
    }
}