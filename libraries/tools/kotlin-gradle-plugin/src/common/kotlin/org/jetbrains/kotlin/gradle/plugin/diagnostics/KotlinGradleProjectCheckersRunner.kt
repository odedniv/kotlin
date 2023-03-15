/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.utils.getOrPut

internal class KotlinGradleProjectCheckersRunner(private val project: Project, private val checkers: List<KotlinGradleProjectChecker>) {
    fun runChecks() {
        val context = KotlinGradleProjectCheckerContext(
            project,
            project.kotlinPropertiesProvider,
            project.multiplatformExtensionOrNull
        )
        val collector = project.kotlinToolingDiagnosticsCollector

        for (checker in checkers) {
            with(checker) { context.runChecks(collector) }
        }
    }

    companion object {
        val ID = "kotlin.${KotlinGradleProjectCheckersRunner::class.simpleName!!}"
    }
}

internal val Project.kotlinGradleProjectCheckersRunner: KotlinGradleProjectCheckersRunner
    get() = extraProperties.getOrPut(KotlinGradleProjectCheckersRunner.ID) {
        KotlinGradleProjectCheckersRunner(this, KotlinGradleProjectChecker.ALL_CHECKERS)
    }
