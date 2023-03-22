/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.scopes.LLFirGlobalSearchScopeFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.hasMetadataAnnotation
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.java.structure.classId
import org.jetbrains.kotlin.utils.mapToIndex

/**
 * [LLFirCombinedJavaSymbolProvider] combines multiple [JavaSymbolProvider]s with the following advantages:
 *
 * - TODO (marco)
 *
 * [combinedScope] must be a scope which combines the scopes of the individual [providers].
 */
internal class LLFirCombinedJavaSymbolProvider(
    session: FirSession,
    private val project: Project,
    private val providers: List<JavaSymbolProvider>,
    private val combinedScope: GlobalSearchScope,
) : FirSymbolProvider(session) {
    private val providersByKtModule: Map<KtModule, JavaSymbolProvider> =
        providers
            .groupBy { it.session.llFirModuleData.ktModule }
            .mapValues { (module, list) -> list.singleOrNull() ?: error("$module must have a unique Java symbol provider.") }

    /**
     * [KtModule] precedence must be checked in case the index finds multiple elements and classpath order needs to be preserved.
     */
    private val modulePrecedences: Map<KtModule, Int> = providers.map { it.session.llFirModuleData.ktModule }.mapToIndex()

    private val javaClassFinder: JavaClassFinder = project.createJavaClassFinder(combinedScope)

    /**
     * Cache [ProjectStructureProvider] to avoid service access when getting [KtModule]s.
     */
    private val projectStructureProvider: ProjectStructureProvider = project.getService(ProjectStructureProvider::class.java)

    // TODO (marco): Comment: The purpose is to avoid index access for frequently accessed elements, hence the LRU cache. Individual results
    //               are still cached in the individual `JavaSymbolProvider`s.
    // TODO (marco): Is this cache thread-safe?
//    private val classCache: SLRUMap<ClassId, Optional<FirRegularClassSymbol>> = SLRUMap(250, 250)

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        // TODO (marco): Maybe we shouldn't store nulls here, but have a separate set which records not-included names in some LRU set.
//        classCache.get(classId)?.let { return it.getOrNull() }

        val javaClasses = javaClassFinder.findClasses(classId)
        if (javaClasses.isEmpty()) return null

        // Find the `KtClassLikeDeclaration` with the highest module precedence. (We're using a custom implementation instead of `minBy` so
        // that `ktModule` doesn't need to be fetched twice.)
        // TODO (marco): Share this with other combined symbol providers.
        var javaClassCandidate: JavaClass? = null
        var currentPrecedence: Int = Int.MAX_VALUE
        var ktModuleCandidate: KtModule? = null

        javaClasses.forEach { javaClass ->
            val psiClass = (javaClass as? JavaClassImpl)?.psi ?: return getClassLikeSymbolWithFallback(classId)
            val ktModule = projectStructureProvider.getKtModuleForKtElement(psiClass)

            // If `candidateKtModule` cannot be found in the map, `javaClass` cannot be processed by any of the available providers, because
            // none of them belong to the correct module. We can skip in that case, because iterating through all providers wouldn't lead to
            // any results for `javaClass`.
            val precedence = modulePrecedences[ktModule] ?: return@forEach
            if (precedence < currentPrecedence) {
                javaClassCandidate = javaClass
                currentPrecedence = precedence
                ktModuleCandidate = ktModule
            }
        }

        val javaClass = javaClassCandidate?.takeIf { it.classId == classId && !it.hasMetadataAnnotation() } ?: return null

        // The provider will always be found at this point, because `modulePrecedences` contains the same keys as `providersByKtModule`
        // and a precedence for `ktModule` must have been found in the previous step.
        val result = providersByKtModule[ktModuleCandidate]!!.getClassLikeSymbolByClassId(classId, javaClass)
//        classCache.put(classId, Optional.ofNullable(result))
        return result
    }

    private fun getClassLikeSymbolWithFallback(classId: ClassId): FirClassLikeSymbol<*>? {
        return providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    override fun getPackage(fqName: FqName): FqName? = providers.firstNotNullOfOrNull { it.getPackage(fqName) }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null
    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? = null
    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null

    companion object {
        fun merge(session: FirSession, project: Project, providers: List<JavaSymbolProvider>): FirSymbolProvider? =
            if (providers.size > 1) {
                val dependencyModules = providers.map { it.session.llFirModuleData.ktModule }
                val combinedScope = LLFirGlobalSearchScopeFactory.getInstance(project).combinedModulesScope(dependencyModules)
                LLFirCombinedJavaSymbolProvider(session, project, providers, combinedScope)
            } else providers.singleOrNull()
    }
}
