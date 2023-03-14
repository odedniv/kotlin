/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.AbstractMessageLite
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.protobuf.Parser
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.compact
import org.jetbrains.kotlin.utils.mapToIndex
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

abstract class DeserializedMemberScope protected constructor(
    protected val c: DeserializationContext,
    functionList: List<ProtoBuf.Function>,
    propertyList: List<ProtoBuf.Property>,
    typeAliasList: List<ProtoBuf.TypeAlias>,
    classNames: () -> Collection<Name>
) : MemberScopeImpl() {
    internal val classNames by c.storageManager.createLazyValue { classNames().toSet() }

    private val classifierNamesLazy by c.storageManager.createNullableLazyValue {
        val nonDeclaredNames = getNonDeclaredClassifierNames() ?: return@createNullableLazyValue null
        this.classNames + typeAliasNames + nonDeclaredNames
    }

    override fun getFunctionNames() = functionNames
    override fun getVariableNames() = variableNames
    override fun getClassifierNames(): Set<Name>? = classifierNamesLazy

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return name !in functionNames && name !in variableNames && name !in classNames && name !in typeAliasNames
    }

    /**
     * Can be overridden to filter specific declared functions. Not called on non-declared functions.
     */
    protected open fun isDeclaredFunctionAvailable(function: SimpleFunctionDescriptor): Boolean = true

    /**
     * This function has the next contract:
     *
     * * Before the call, [declaredFunctions] should already contain all declared functions with the [name] name.
     */
    protected open fun computeNonDeclaredFunctions(
        name: Name,
        declaredFunctions: List<SimpleFunctionDescriptor>
    ): List<SimpleFunctionDescriptor> {
        return emptyList()
    }

    /**
     * This function has the next contract:
     *
     * * Before the call, [declaredProperties] should already contain all declared properties with the [name] name.
     */
    protected open fun computeNonDeclaredProperties(name: Name, declaredProperties: List<PropertyDescriptor>): List<PropertyDescriptor> {
        return emptyList()
    }

    protected fun computeDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        //NOTE: descriptors should be in the same order they were serialized in
        // see MemberComparator
        val result = ArrayList<DeclarationDescriptor>(0)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.SINGLETON_CLASSIFIERS_MASK)) {
            addEnumEntryDescriptors(result, nameFilter)
        }

        addDeclaredFunctionsAndPropertiesTo(result, kindFilter, nameFilter)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            for (className in classNames) {
                if (nameFilter(className)) {
                    result.addIfNotNull(deserializeClass(className))
                }
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.TYPE_ALIASES_MASK)) {
            for (typeAliasName in typeAliasNames) {
                if (nameFilter(typeAliasName)) {
                    result.addIfNotNull(typeAliasByName(typeAliasName))
                }
            }
        }

        addNonDeclaredFunctionsAndPropertiesTo(result, kindFilter, nameFilter)

        return result.compact()
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        when {
            hasClass(name) -> deserializeClass(name)
            name in typeAliasNames -> typeAliasByName(name)
            else -> null
        }

    private fun deserializeClass(name: Name): ClassDescriptor? =
        c.components.deserializeClass(createClassId(name))

    protected open fun hasClass(name: Name): Boolean =
        name in classNames

    protected abstract fun createClassId(name: Name): ClassId

    protected abstract fun getNonDeclaredFunctionNames(): Set<Name>
    protected abstract fun getNonDeclaredVariableNames(): Set<Name>
    protected abstract fun getNonDeclaredClassifierNames(): Set<Name>?

    protected abstract fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean)

    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.println("containingDeclaration = " + c.containingDeclaration)

        p.popIndent()
        p.println("}")
    }

    private val declaredFunctions = functionList.pack(
        { it.name },
        ProtoBuf.Function.PARSER,
        { c.memberDeserializer.loadFunction(it).takeIf(::isDeclaredFunctionAvailable) }
    )

    private val declaredProperties = propertyList.pack(
        { it.name },
        ProtoBuf.Property.PARSER,
        { c.memberDeserializer.loadProperty(it) }
    )

    private val typeAliasBytes = runIf(c.components.configuration.typeAliasesAllowed) {
        typeAliasList.pack(
            { it.name },
            ProtoBuf.TypeAlias.PARSER,
            { c.memberDeserializer.loadTypeAlias(it) }
        )
    }

    private inner class PackedDeclarations<M : MessageLite, T : DeclarationDescriptor>(
        private val bytes: List<ByteArray>,
        val indexByName: Map<Name, List<Int>>,
        private val parser: Parser<M>,
        private val factory: (M) -> T?
    ) : Iterable<T> {
        private val descriptors = c.storageManager.createMemoizedFunctionWithNullableValues<Int, T> {
            val inputStream = ByteArrayInputStream(bytes[it])
            val proto = parser.parseDelimitedFrom(inputStream, c.components.extensionRegistryLite)
            factory(proto)
        }

        operator fun get(name: Name): List<T> = indexByName[name]
            ?.mapNotNull { descriptors(it) }
            ?: emptyList()

        override fun iterator(): Iterator<T> {
            return bytes.indices.asSequence().mapNotNull { descriptors(it) }.iterator()
        }
    }

    private inline fun <M : AbstractMessageLite, D : DeclarationDescriptor> Collection<M>.pack(
        name: (M) -> Int,
        parser: Parser<M>,
        noinline factory: (M) -> D?
    ): PackedDeclarations<M, D> {
        val indexByMessage = this.mapToIndex()
        val bytes = this.map { proto ->
            val byteArrayOutputStream = ByteArrayOutputStream()
            proto.writeDelimitedTo(byteArrayOutputStream)
            byteArrayOutputStream.toByteArray()
        }
        val indexByName = this.groupByName { name(it) }.mapValues { (_, protos) ->
            protos.map { indexByMessage.getValue(it) }
        }
        return PackedDeclarations(bytes, indexByName, parser, factory)
    }

    private val nonDeclaredFunctions = c.storageManager.createMemoizedFunction<Name, List<SimpleFunctionDescriptor>> { name ->
        computeNonDeclaredFunctions(name, declaredFunctions[name])
    }

    private val nonDeclaredProperties = c.storageManager.createMemoizedFunction<Name, List<PropertyDescriptor>> { name ->
        computeNonDeclaredProperties(name, declaredProperties[name])
    }

    private val typeAliasByName =
        c.storageManager.createMemoizedFunctionWithNullableValues<Name, TypeAliasDescriptor> { typeAliasBytes?.get(it)?.singleOrNull() }

    @get:JvmName("functionNamesValue")
    private val functionNames by c.storageManager.createLazyValue { declaredFunctions.indexByName.keys + nonDeclaredFunctionNames }

    @get:JvmName("nonDeclaredFunctionNamesValues")
    private val nonDeclaredFunctionNames by c.storageManager.createLazyValue { getNonDeclaredFunctionNames() }

    @get:JvmName("variableNamesValue")
    private val variableNames by c.storageManager.createLazyValue { declaredProperties.indexByName.keys + nonDeclaredVariableNames }

    @get:JvmName("nonDeclaredVariableNamesValue")
    private val nonDeclaredVariableNames by c.storageManager.createLazyValue { getNonDeclaredVariableNames() }

    private val typeAliasNames: Set<Name> get() = typeAliasBytes?.indexByName?.keys ?: emptySet()

    private inline fun <M : MessageLite> Collection<M>.groupByName(
        getNameIndex: (M) -> Int
    ): Map<Name, List<M>> = groupBy { c.nameResolver.getName(getNameIndex(it)) }

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        if (name !in functionNames) return emptyList()
        val declared = declaredFunctions[name]
        if (name !in nonDeclaredFunctionNames) return declared
        return declared + nonDeclaredFunctions(name)
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        if (name !in variableNames) return emptyList()
        val declared = declaredProperties[name]
        if (name !in nonDeclaredVariableNames) return declared
        return declared + nonDeclaredProperties(name)
    }

    private fun addDeclaredFunctionsAndPropertiesTo(
        result: MutableCollection<DeclarationDescriptor>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) {
        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            declaredProperties.filterTo(result) { nameFilter(it.name) }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            declaredFunctions.filterTo(result) { nameFilter(it.name) }
        }
    }

    private fun addNonDeclaredFunctionsAndPropertiesTo(
        result: MutableCollection<DeclarationDescriptor>,
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ) {
        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            nonDeclaredVariableNames.filter(nameFilter).forEach { name -> result.addAll(nonDeclaredProperties(name)) }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            nonDeclaredFunctionNames.filter(nameFilter).forEach { name -> result.addAll(nonDeclaredFunctions(name)) }
        }
    }
}
