/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.CommonMainWithDependsOnChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.DeprecatedKotlinNativeTargetsChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.MissingNativeStdlibChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.UnusedSourceSetsChecker

internal interface KotlinGradleProjectChecker {
    fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector)

    companion object {
        val ALL_CHECKERS: List<KotlinGradleProjectChecker> = listOf(
            CommonMainWithDependsOnChecker,
            DeprecatedKotlinNativeTargetsChecker,
            MissingNativeStdlibChecker,
            UnusedSourceSetsChecker,
        )
    }
}

internal open class KotlinGradleProjectCheckerContext(
    val project: Project,
    val kotlinPropertiesProvider: PropertiesProvider,
    val multiplatformExtension: KotlinMultiplatformExtension?,
)
