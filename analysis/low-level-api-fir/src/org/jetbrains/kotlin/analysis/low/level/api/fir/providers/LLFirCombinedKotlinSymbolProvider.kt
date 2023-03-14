/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirSymbolProviderNameCache
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.mapToIndex

/**
 * [LLFirCombinedKotlinSymbolProvider] combines multiple [LLFirProvider.SymbolProvider]s with the following advantages:
 *
 * - For a given class or callable ID, indices can be accessed once to get relevant PSI elements. Then the correct symbol provider(s) to
 *   call can be found out via the PSI element's [KtModule]s. This avoids the need to call every single subordinate symbol provider.
 * - TODO (marco): A name cache/set can be checked once instead of for each symbol provider. Because these symbol providers are usually
 *                 ordered first in [LLFirDependenciesSymbolProvider], this check should be especially fruitful. (For example, it would make
 *                 no sense were the provider ordered last, as most of the time a symbol should be found.)
 * - TODO (marco): Another layer of caching can be provided, perhaps with an LRU cache.
 */
internal class LLFirCombinedKotlinSymbolProvider(
    session: FirSession,
    private val project: Project,
    private val providers: List<LLFirProvider.SymbolProvider>,
    private val declarationProvider: KotlinDeclarationProvider,
) : FirSymbolProvider(session) {
    private val providersByKtModule: Map<KtModule, LLFirProvider.SymbolProvider> =
        providers
            .groupBy { it.session.llFirModuleData.ktModule }
            .mapValues { (module, list) -> list.singleOrNull() ?: error("$module must have a unique Kotlin symbol providers.") }

    /**
     * [KtModule] precedence must be checked in case the index finds multiple elements and classpath order needs to be preserved.
     */
    private val modulePrecedenceMap: Map<KtModule, Int> = providers.map { it.session.llFirModuleData.ktModule }.mapToIndex()

    /**
     * Cache [ProjectStructureProvider] to avoid service access when getting [KtModule]s.
     */
    private val projectStructureProvider: ProjectStructureProvider = project.getService(ProjectStructureProvider::class.java)

    // TODO (marco): Take names from index or build set from individual providers? My theory is that building sets from individual providers
    //               will be faster because the providers may already have computed these names when they're called from some other
    //               dependent module or as a self providers of a session.
    // TODO (marco): Measure memory consumption in `intellij` project with this option enabled and disabled. IF memory consumption is an
    //               issue, perhaps we can turn the caches in `LLFirSymbolProviderNameCache` into weak maps.
    val symbolNameCache = object : LLFirSymbolProviderNameCache(session) {
        override fun computeClassifierNames(packageFqName: FqName): Set<String>? = buildSet {
            // declarationProvider
            //     .getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)
            //     .mapTo(mutableSetOf()) { it.asString() }
            providers.forEach { addAll(it.knownTopLevelClassifiersInPackage(packageFqName) ?: return null) }
        }

        override fun computeCallableNames(packageFqName: FqName): Set<Name>? = buildSet {
            // declarationProvider.getTopLevelCallableNamesInPackage(packageFqName)
            providers.forEach { addAll(it.computeCallableNamesInPackage(packageFqName) ?: return null) }
        }
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (!symbolNameCache.mayHaveTopLevelClassifier(classId, mayHaveFunctionClass = false)) return null

        val candidates = declarationProvider.getAllClassesByClassId(classId) + declarationProvider.getAllTypeAliasesByClassId(classId)
        if (candidates.isEmpty()) return null

        // Find the `KtClassLikeDeclaration` with the highest module precedence. (We're using a custom implementation instead of `minBy` so
        // that `ktModule` doesn't need to be fetched twice.)
        // TODO (marco): This algorithm can be applied to other combined symbol providers as well, such as Java symbol providers.
        var ktClassCandidate: KtClassLikeDeclaration? = null
        var currentPrecedence: Int = Int.MAX_VALUE
        var ktModule: KtModule? = null

        candidates.forEach { candidate ->
            val candidateKtModule = projectStructureProvider.getKtModuleForKtElement(candidate)

            // If `candidateKtModule` cannot be found in the map, `candidate` cannot be processed by any of the available providers, because
            // none of them belong to the correct module. We can skip in that case, because iterating through all providers wouldn't lead to
            // any results for `candidate`.
            val precedence = modulePrecedenceMap[candidateKtModule] ?: return@forEach
            if (precedence < currentPrecedence) {
                ktClassCandidate = candidate
                currentPrecedence = precedence
                ktModule = candidateKtModule
            }
        }

        val ktClass = ktClassCandidate ?: return null

        // The provider will always be found at this point, because `modulePrecedenceMap` contains the same keys as `providersByKtModule`
        // and a precedence for `ktModule` must have been found in the previous step.
        return providersByKtModule[ktModule]!!.getClassLikeSymbolByClassId(classId, ktClass)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        forEachCallableProvider(packageFqName, name) { callableId, callableFiles ->
            getTopLevelCallableSymbolsTo(destination, callableId, callableFiles)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        forEachCallableProvider(packageFqName, name) { callableId, callableFiles ->
            getTopLevelFunctionSymbolsTo(destination, callableId, callableFiles)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        forEachCallableProvider(packageFqName, name) { callableId, callableFiles ->
            getTopLevelPropertySymbolsTo(destination, callableId, callableFiles)
        }
    }

    /**
     * Calls [provide] on those providers which can contribute a callable of the given name.
     */
    private fun forEachCallableProvider(
        packageFqName: FqName,
        name: Name,
        provide: LLFirProvider.SymbolProvider.(CallableId, Collection<KtFile>) -> Unit,
    ) {
        if (!symbolNameCache.mayHaveTopLevelCallable(packageFqName, name)) return

        val callableId = CallableId(packageFqName, name)

        declarationProvider.getTopLevelCallableFiles(callableId)
            .groupBy { it.getKtModule(project) }
            .forEach { (ktModule, ktFiles) ->
                // If `ktModule` cannot be found in the map, `ktFiles` cannot be processed by any of the available providers, because none
                // of them belong to the correct module. We can skip in that case, because iterating through all providers wouldn't lead to
                // any results for `ktFiles`.
                val provider = providersByKtModule[ktModule] ?: return@forEach
                provider.provide(callableId, ktFiles)
            }
    }

    override fun getPackage(fqName: FqName): FqName? = providers.firstNotNullOfOrNull { it.getPackage(fqName) }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null

    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? =
        symbolNameCache.getTopLevelClassifierNamesInPackage(packageFqName)

    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        symbolNameCache.getTopLevelCallableNamesInPackage(packageFqName)
}
