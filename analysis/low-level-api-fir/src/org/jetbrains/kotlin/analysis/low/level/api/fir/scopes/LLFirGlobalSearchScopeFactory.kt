/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.scopes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule

/**
 * [LLFirGlobalSearchScopeFactory] provides [GlobalSearchScope] capabilities which aren't directly accessible from `kotlin` sources.
 */
interface LLFirGlobalSearchScopeFactory {
    /**
     * Builds an optimized [GlobalSearchScope] that contains the source scopes of all [modules]. Indirect dependencies are not included in
     * the combined scope.
     */
    fun combinedModulesScope(modules: List<KtModule>): GlobalSearchScope

    companion object {
        fun getInstance(project: Project): LLFirGlobalSearchScopeFactory =
            project.getService(LLFirGlobalSearchScopeFactory::class.java) ?: LLFirGlobalSearchScopeFactoryDefaultImpl
    }
}

private object LLFirGlobalSearchScopeFactoryDefaultImpl : LLFirGlobalSearchScopeFactory {
    override fun combinedModulesScope(modules: List<KtModule>): GlobalSearchScope =
        GlobalSearchScope.union(modules.map { it.contentScope })
}
