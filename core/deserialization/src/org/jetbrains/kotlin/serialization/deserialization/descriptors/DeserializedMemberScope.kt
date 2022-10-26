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
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext
import org.jetbrains.kotlin.serialization.deserialization.MemberDeserializer
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

    private val impl: Implementation = createImplementation(functionList, propertyList, typeAliasList)

    internal val classNames by c.storageManager.createLazyValue { classNames().toSet() }


    private val classifierNamesLazy by c.storageManager.createNullableLazyValue {
        val nonDeclaredNames = getNonDeclaredClassifierNames() ?: return@createNullableLazyValue null
        this.classNames + impl.typeAliasNames + nonDeclaredNames
    }

    override fun getFunctionNames() = impl.functionNames
    override fun getVariableNames() = impl.variableNames
    override fun getClassifierNames(): Set<Name>? = classifierNamesLazy

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return name !in impl.functionNames && name !in impl.variableNames && name !in classNames && name !in impl.typeAliasNames
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

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
        return impl.getContributedFunctions(name, location)
    }

    /**
     * This function has the next contract:
     *
     * * Before the call, [declaredProperties] should already contain all declared properties with the [name] name.
     */
    protected open fun computeNonDeclaredProperties(name: Name, declaredProperties: List<PropertyDescriptor>): List<PropertyDescriptor> {
        return emptyList()
    }

    private fun getTypeAliasByName(name: Name): TypeAliasDescriptor? {
        return impl.getTypeAliasByName(name)
    }

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        return impl.getContributedVariables(name, location)
    }

    protected fun computeDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean,
        location: LookupLocation
    ): Collection<DeclarationDescriptor> {
        //NOTE: descriptors should be in the same order they were serialized in
        // see MemberComparator
        val result = ArrayList<DeclarationDescriptor>(0)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.SINGLETON_CLASSIFIERS_MASK)) {
            addEnumEntryDescriptors(result, nameFilter)
        }

        impl.addDeclaredFunctionsAndPropertiesTo(result, kindFilter, nameFilter, location)

        if (kindFilter.acceptsKinds(DescriptorKindFilter.CLASSIFIERS_MASK)) {
            for (className in classNames) {
                if (nameFilter(className)) {
                    result.addIfNotNull(deserializeClass(className))
                }
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.TYPE_ALIASES_MASK)) {
            for (typeAliasName in impl.typeAliasNames) {
                if (nameFilter(typeAliasName)) {
                    result.addIfNotNull(impl.getTypeAliasByName(typeAliasName))
                }
            }
        }

        impl.addNonDeclaredFunctionsAndPropertiesTo(result, kindFilter, nameFilter, location)

        return result.compact()
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
        when {
            hasClass(name) -> deserializeClass(name)
            name in impl.typeAliasNames -> getTypeAliasByName(name)
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

    /**
     * This interface was introduced to fix KT-41346
     *
     * The first implementation, [OptimizedImplementation], is more space-efficient and performant. It does not
     * preserve the order of declarations in [addDeclaredFunctionsAndPropertiesTo] though, and have to restore it manually. It is used
     * in most situations when the [DeserializedMemberScope] is created.
     *
     * The second implementation, [NoReorderImplementation], is less efficient, but it keeps the descriptors
     * in the same order as in serialized ProtoBuf objects in [addDeclaredFunctionsAndPropertiesTo]. It should be used only when
     * [org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration.preserveDeclarationsOrdering] is
     * set to `true`, which is done during decompilation from deserialized descriptors.
     *
     * The decompiled descriptors are used to build PSI, which is then compared with PSI built directly from classfiles and metadata.
     *
     * If the declarations in the first and the second PSI go in a different order, PSI-Stub mismatch error is raised.
     *
     * PSI from classfiles and metadata uses the same order of the declarations as in the serialized ProtoBuf objects.
     * This order is dictated by [MemberComparator].
     *
     * [OptimizedImplementation] uses [MemberComparator.NameAndTypeMemberComparator] to restore the same order
     * of the declarations as it should be in serialized objects. However, this does not always work (for example, when
     * the Kotlin classes were obfuscated by ProGuard).
     *
     * ProGuard may rename some declarations in serialized objects, and then the comparator will reorder them based on their new names.
     * This will lead to PSI-Stub mismatch error since the declarations are now differently ordered.
     *
     * To avoid this, we have [NoReorderImplementation] implementation. It performs no reordering of the declarations at
     * all. Since it is less space-efficient, it is used only the scope is going to be used during decompilation.

     * [createImplementation] is used to create the correct implementation of [Implementation].
     *
     * Both [OptimizedImplementation] and [NoReorderImplementation] are made inner classes to have
     * access to protected `getNonDeclared*` functions.
     */
    private interface Implementation {
        val functionNames: Set<Name>
        val variableNames: Set<Name>
        val typeAliasNames: Set<Name>

        fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor>
        fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor>
        fun getTypeAliasByName(name: Name): TypeAliasDescriptor?

        fun addDeclaredFunctionsAndPropertiesTo(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
        )

        fun addNonDeclaredFunctionsAndPropertiesTo(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
        )
    }

    private fun createImplementation(
        functionList: List<ProtoBuf.Function>,
        propertyList: List<ProtoBuf.Property>,
        typeAliasList: List<ProtoBuf.TypeAlias>
    ): Implementation =
        if (c.components.configuration.preserveDeclarationsOrdering)
            NoReorderImplementation(functionList, propertyList, typeAliasList)
        else
            OptimizedImplementation(functionList, propertyList, typeAliasList)

    private inner class OptimizedImplementation(
        functionList: List<ProtoBuf.Function>,
        propertyList: List<ProtoBuf.Property>,
        typeAliasList: List<ProtoBuf.TypeAlias>
    ) : Implementation {
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

        override val functionNames by c.storageManager.createLazyValue { declaredFunctions.indexByName.keys + nonDeclaredFunctionNames }
        private val nonDeclaredFunctionNames by c.storageManager.createLazyValue { getNonDeclaredFunctionNames() }

        override val variableNames by c.storageManager.createLazyValue { declaredProperties.indexByName.keys + nonDeclaredVariableNames }
        private val nonDeclaredVariableNames by c.storageManager.createLazyValue { getNonDeclaredVariableNames() }

        override val typeAliasNames: Set<Name> get() = typeAliasBytes?.indexByName?.keys ?: emptySet()

        private inline fun <M : MessageLite> Collection<M>.groupByName(
            getNameIndex: (M) -> Int
        ) = groupBy { c.nameResolver.getName(getNameIndex(it)) }

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
            if (name !in functionNames) return emptyList()
            val declared = declaredFunctions[name]
            if (name !in nonDeclaredFunctionNames) return declared
            return declared + nonDeclaredFunctions(name)
        }

        override fun getTypeAliasByName(name: Name): TypeAliasDescriptor? {
            return typeAliasByName(name)
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            if (name !in variableNames) return emptyList()
            val declared = declaredProperties[name]
            if (name !in nonDeclaredVariableNames) return declared
            return declared + nonDeclaredProperties(name)
        }

        override fun addDeclaredFunctionsAndPropertiesTo(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
        ) {
            if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
                declaredProperties.filterTo(result) { nameFilter(it.name) }
            }

            if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
                declaredFunctions.filterTo(result) { nameFilter(it.name) }
            }
        }

        override fun addNonDeclaredFunctionsAndPropertiesTo(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
        ) {
            if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
                nonDeclaredVariableNames.filter(nameFilter).forEach { name -> result.addAll(nonDeclaredProperties(name)) }
            }

            if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
                nonDeclaredFunctionNames.filter(nameFilter).forEach { name -> result.addAll(nonDeclaredFunctions(name)) }
            }
        }
    }

    /**
     * Take a note that [NoReorderImplementation] still adds non-declared members together with directly declared in class.
     * This is not a problem for ordering, since during decompilation from descriptors those non-declared members are just ignored,
     * and the declared members will be added to decompiled text in the proper (i.e. original) order.
     */
    private inner class NoReorderImplementation(
        private val functionList: List<ProtoBuf.Function>,
        private val propertyList: List<ProtoBuf.Property>,
        typeAliasList: List<ProtoBuf.TypeAlias>
    ) : Implementation {

        private val typeAliasList = if (c.components.configuration.typeAliasesAllowed) typeAliasList else emptyList()

        private val declaredFunctions: List<SimpleFunctionDescriptor>
                by c.storageManager.createLazyValue { computeFunctions() }

        private val declaredProperties: List<PropertyDescriptor>
                by c.storageManager.createLazyValue { computeProperties() }

        private val nonDeclaredFunctions: List<SimpleFunctionDescriptor>
                by c.storageManager.createLazyValue { computeAllNonDeclaredFunctions() }

        private val nonDeclaredProperties: List<PropertyDescriptor>
                by c.storageManager.createLazyValue { computeAllNonDeclaredProperties() }

        private val allTypeAliases: List<TypeAliasDescriptor>
                by c.storageManager.createLazyValue { computeTypeAliases() }

        private val allFunctions: List<SimpleFunctionDescriptor>
                by c.storageManager.createLazyValue { declaredFunctions + nonDeclaredFunctions }

        private val allProperties: List<PropertyDescriptor>
                by c.storageManager.createLazyValue { declaredProperties + nonDeclaredProperties }

        private val typeAliasesByName: Map<Name, TypeAliasDescriptor>
                by c.storageManager.createLazyValue { allTypeAliases.associateBy { it.name } }

        private val functionsByName: Map<Name, Collection<SimpleFunctionDescriptor>>
                by c.storageManager.createLazyValue { allFunctions.groupBy { it.name } }

        private val propertiesByName: Map<Name, Collection<PropertyDescriptor>>
                by c.storageManager.createLazyValue { allProperties.groupBy { it.name } }

        override val functionNames by c.storageManager.createLazyValue {
            functionList.mapToNames { it.name } + getNonDeclaredFunctionNames()
        }

        override val variableNames by c.storageManager.createLazyValue {
            propertyList.mapToNames { it.name } + getNonDeclaredVariableNames()
        }

        override val typeAliasNames: Set<Name>
            get() = typeAliasList.mapToNames { it.name }

        private fun computeFunctions(): List<SimpleFunctionDescriptor> =
            functionList.mapWithDeserializer { loadFunction(it).takeIf(::isDeclaredFunctionAvailable) }

        private fun computeProperties(): List<PropertyDescriptor> =
            propertyList.mapWithDeserializer { loadProperty(it) }

        private fun computeTypeAliases(): List<TypeAliasDescriptor> =
            typeAliasList.mapWithDeserializer { loadTypeAlias(it) }

        private fun computeAllNonDeclaredFunctions(): List<SimpleFunctionDescriptor> =
            getNonDeclaredFunctionNames().flatMap { computeNonDeclaredFunctionsForName(it) }

        private fun computeAllNonDeclaredProperties(): List<PropertyDescriptor> =
            getNonDeclaredVariableNames().flatMap { computeNonDeclaredPropertiesForName(it) }

        private fun computeNonDeclaredFunctionsForName(name: Name): List<SimpleFunctionDescriptor> =
            computeNonDeclaredDescriptors(name, declaredFunctions, ::computeNonDeclaredFunctions)

        private fun computeNonDeclaredPropertiesForName(name: Name): List<PropertyDescriptor> =
            computeNonDeclaredDescriptors(name, declaredProperties, ::computeNonDeclaredProperties)

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
            if (name !in functionNames) return emptyList()
            return functionsByName[name].orEmpty()
        }

        override fun getTypeAliasByName(name: Name): TypeAliasDescriptor? {
            return typeAliasesByName[name]
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            if (name !in variableNames) return emptyList()
            return propertiesByName[name].orEmpty()
        }

        override fun addDeclaredFunctionsAndPropertiesTo(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
        ) {
            if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
                declaredProperties.filterTo(result) { nameFilter(it.name) }
            }

            if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
                declaredFunctions.filterTo(result) { nameFilter(it.name) }
            }
        }

        override fun addNonDeclaredFunctionsAndPropertiesTo(
            result: MutableCollection<DeclarationDescriptor>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            location: LookupLocation
        ) {
            if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
                nonDeclaredProperties.filterTo(result) { nameFilter(it.name) }
            }

            if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
                nonDeclaredFunctions.filterTo(result) { nameFilter(it.name) }
            }
        }

        /**
         * We have to collect non-declared properties in such non-pretty way because we don't want to change the contract of the
         * [computeNonDeclaredProperties] and [computeNonDeclaredFunctions] methods, because we do not want any performance penalties.
         *
         * [computeNonDeclared] may only add elements to the end of [MutableList], otherwise this function would not work properly.
         */
        private inline fun <T : DeclarationDescriptor> computeNonDeclaredDescriptors(
            name: Name,
            declaredDescriptors: List<T>,
            computeNonDeclared: (Name, MutableList<T>) -> Unit
        ): List<T> {
            val declaredDescriptorsWithSameName = declaredDescriptors.filterTo(mutableListOf()) { it.name == name }
            val nonDeclaredPropertiesStartIndex = declaredDescriptorsWithSameName.size

            computeNonDeclared(name, declaredDescriptorsWithSameName)

            return declaredDescriptorsWithSameName.subList(nonDeclaredPropertiesStartIndex, declaredDescriptorsWithSameName.size)
        }

        private inline fun <T : MessageLite> List<T>.mapToNames(getNameIndex: (T) -> Int): Set<Name> {
            // `mutableSetOf` returns `LinkedHashSet`, it is important to preserve the order of the declarations.
            return mapTo(mutableSetOf()) { c.nameResolver.getName(getNameIndex(it)) }
        }

        private inline fun <T : MessageLite, K : MemberDescriptor> List<T>.mapWithDeserializer(
            deserialize: MemberDeserializer.(T) -> K?
        ): List<K> {
            return mapNotNull { c.memberDeserializer.deserialize(it) }
        }
    }
}
