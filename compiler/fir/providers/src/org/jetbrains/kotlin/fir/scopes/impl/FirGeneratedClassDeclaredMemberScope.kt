/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.validate
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.ownerGenerator
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirGeneratedClassDeclaredMemberScope private constructor(
    val useSiteSession: FirSession,
    private val generationContext: MemberGenerationContext,
    needNestedClassifierScope: Boolean,
    val extensionsByCallableName: Map<Name, List<FirDeclarationGenerationExtension>>,
    val allCallableNames: Set<Name>
) : FirClassDeclaredMemberScope(generationContext.owner.classId) {
    companion object {
        fun create(
            session: FirSession,
            generationContext: MemberGenerationContext,
            needNestedClassifierScope: Boolean
        ): FirGeneratedClassDeclaredMemberScope? {
            val extensionsByCallableName = groupExtensionsByName(
                generationContext.owner.fir,
                nameExtractor = { getCallableNamesForClass(it, generationContext) }
            ) { it }
            val allCallableNames = extensionsByCallableName.keys
            if (allCallableNames.isEmpty()) return null
            return FirGeneratedClassDeclaredMemberScope(
                session,
                generationContext,
                needNestedClassifierScope,
                extensionsByCallableName,
                allCallableNames
            )
        }
    }

    private val firClass: FirClass
        get() = generationContext.owner.fir

    private val nestedClassifierScope: FirNestedClassifierScope? = runIf(needNestedClassifierScope) {
        useSiteSession.nestedClassifierScope(firClass)
    }

    // ------------------------------------------ caches ------------------------------------------

    private val cache = firClass.moduleData.session.generatedDeclarationsCache.cacheByClass.getValue(firClass.symbol)

    // ------------------------------------------ generators ------------------------------------------

    internal fun generateMemberFunctions(name: Name): List<FirNamedFunctionSymbol> {
        if (name == SpecialNames.INIT) return emptyList()
        return extensionsByCallableName[name].orEmpty()
            .flatMap { it.generateFunctions(CallableId(firClass.classId, name), generationContext) }
            .onEach { it.fir.validate() }
    }

    internal fun generateMemberProperties(name: Name): List<FirPropertySymbol> {
        if (name == SpecialNames.INIT) return emptyList()
        return extensionsByCallableName[name].orEmpty()
            .flatMap { it.generateProperties(CallableId(firClass.classId, name), generationContext) }
            .onEach { it.fir.validate() }
    }

    internal fun generateConstructors(): List<FirConstructorSymbol> {
        return extensionsByCallableName[SpecialNames.INIT].orEmpty()
            .flatMap { it.generateConstructors(generationContext) }
            .onEach { it.fir.validate() }
    }

    // ------------------------------------------ scope methods ------------------------------------------

    override fun getCallableNames(): Set<Name> {
        return allCallableNames
    }

    override fun getClassifierNames(): Set<Name> {
        return nestedClassifierScope?.getClassifierNames() ?: emptySet()
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        nestedClassifierScope?.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name !in getCallableNames()) return
        for (functionSymbol in cache.functionCache.getValue(name, this)) {
            processor(functionSymbol)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        if (name !in getCallableNames()) return
        for (propertySymbol in cache.propertyCache.getValue(name, this)) {
            processor(propertySymbol)
        }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        for (constructorSymbol in cache.getConstructors(this)) {
            processor(constructorSymbol)
        }
    }
}

internal inline fun <T, V> groupExtensionsByName(
    klass: FirClass,
    nameExtractor: FirDeclarationGenerationExtension.(FirClassSymbol<*>) -> Set<T>,
    nameTransformer: (T) -> V
): Map<V, List<FirDeclarationGenerationExtension>> {
    val extensions = getExtensionsForClass(klass)
    val symbol = klass.symbol
    return extensions.flatGroupBy(
        keySelector = { extension -> extension.nameExtractor(symbol) },
        keyTransformer = nameTransformer,
        valueTransformer = { it }
    )
}

internal fun getExtensionsForClass(klass: FirClass): List<FirDeclarationGenerationExtension> {
    val extensions = klass.moduleData.session.extensionService.declarationGenerators
    return if (klass.origin.generated) {
        listOf(klass.ownerGenerator!!)
    } else {
        extensions
    }
}

class FirGeneratedClassNestedClassifierScope private constructor(
    useSiteSession: FirSession,
    klass: FirClass,
    private val extensionsByName: Map<Name, List<FirDeclarationGenerationExtension>>,
    private val context: NestedClassGenerationContext
) : FirNestedClassifierScope(klass, useSiteSession) {
    companion object {
        @OptIn(FirExtensionApiInternals::class)
        fun create(
            useSiteSession: FirSession,
            klass: FirClass,
            baseScope: FirNestedClassifierScope?
        ): FirGeneratedClassNestedClassifierScope? {
            val symbol = klass.symbol
            val context = NestedClassGenerationContext(klass.symbol, baseScope)
            val extensionsByName = getExtensionsForClass(klass).flatGroupBy {
                it.nestedClassifierNamesCache.getValue(symbol, context)
            }
            if (extensionsByName.isEmpty()) return null
            return FirGeneratedClassNestedClassifierScope(useSiteSession, klass, extensionsByName, context)
        }
    }

    private val cache = context.owner.moduleData.session.generatedDeclarationsCache.cacheByClass.getValue(context.owner)

    internal fun generateNestedClassifier(name: Name): FirRegularClassSymbol? {
        if (klass is FirRegularClass) {
            val companion = klass.companionObjectSymbol
            if (companion != null && companion.origin.generated && companion.classId.shortClassName == name) {
                return companion
            }
        }

        val extensions = extensionsByName[name] ?: return null

        val generatedClasses = extensions.mapNotNull { extension ->
            extension.generateNestedClassLikeDeclaration(klass.symbol, name, context)?.also { symbol ->
                symbol.fir.ownerGenerator = extension
            }
        }

        val generatedClass = when (generatedClasses.size) {
            0 -> return null
            1 -> generatedClasses.first()
            else -> error(
                """
                     Multiple plugins generated nested class with same name $name for class ${klass.classId}:
                    ${generatedClasses.joinToString("\n") { it.fir.render() }}
                """.trimIndent()
            )
        }
        require(generatedClass is FirRegularClassSymbol) { "Only regular class are allowed as nested classes" }
        return generatedClass
    }

    override fun getNestedClassSymbol(name: Name): FirRegularClassSymbol? {
        return cache.classifiersCache.getValue(name, this)
    }

    override fun isEmpty(): Boolean {
        return extensionsByName.isEmpty()
    }

    override fun getClassifierNames(): Set<Name> {
        return extensionsByName.keys
    }
}

class FirGeneratedMemberDeclarationsCache(session: FirSession) : FirSessionComponent {
    private val cachesFactory = session.firCachesFactory

    internal val cacheByClass: FirCache<FirClassSymbol<*>, Cache, Nothing?> =
        cachesFactory.createCache { _ -> Cache(cachesFactory) }

    internal class Cache(cachesFactory: FirCachesFactory) {
        val functionCache: FirCache<Name, List<FirNamedFunctionSymbol>, FirGeneratedClassDeclaredMemberScope> =
            cachesFactory.createCache { name, scope -> scope.generateMemberFunctions(name) }

        val propertyCache: FirCache<Name, List<FirPropertySymbol>, FirGeneratedClassDeclaredMemberScope> =
            cachesFactory.createCache { name, scope -> scope.generateMemberProperties(name) }

        private val constructorCache: FirCache<Unit, List<FirConstructorSymbol>, FirGeneratedClassDeclaredMemberScope> =
            cachesFactory.createCache { _, scope -> scope.generateConstructors() }

        fun getConstructors(scope: FirGeneratedClassDeclaredMemberScope): List<FirConstructorSymbol> {
            return constructorCache.getValue(Unit, scope)
        }

        val classifiersCache: FirCache<Name, FirRegularClassSymbol?, FirGeneratedClassNestedClassifierScope> =
            cachesFactory.createCache { name, scope -> scope.generateNestedClassifier(name) }
    }
}

private val FirSession.generatedDeclarationsCache: FirGeneratedMemberDeclarationsCache by FirSession.sessionComponentAccessor()
