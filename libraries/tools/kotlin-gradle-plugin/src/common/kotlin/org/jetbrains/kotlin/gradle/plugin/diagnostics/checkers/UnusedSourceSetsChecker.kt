/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector

internal object UnusedSourceSetsChecker : KotlinGradleProjectChecker {

    override fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        val unusedSourceSets = project.multiplatformExtension.sourceSets
            // Ignoring Android source sets
            .filter { it.androidSourceSetInfoOrNull == null }
            .filter { it.internal.compilations.isEmpty() }

        if (unusedSourceSets.isNotEmpty()) {
            collector.report(project, KotlinToolingDiagnostics.UnusedSourceSetsWarning(unusedSourceSets.toSet().map { it.name }))
        }
    }
}
