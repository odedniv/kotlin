/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.backend.common.lower.parents
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.ir.hasPlatformDependent
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.utils.isEqualsInheritedFromAny
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.intrinsicConstEvaluationAnnotation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_LOCAL_DECLARATION_SIGNATURES
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SIGNATURES
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.MUTE_SIGNATURE_COMPARISON_K2
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.SKIP_SIGNATURE_DUMP
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.same
import org.jetbrains.kotlin.utils.addToStdlib.swap
import java.io.FileNotFoundException
import kotlin.reflect.KMutableProperty1

private const val CHECK_MARKER = "// CHECK"

/**
 * Prints a mangled name and an [IdSignature] for each declaration and compares the result with
 * an expected output in a `*.sig.kt.txt` file located next to the test file.
 *
 * Can be enabled by specifying the [DUMP_SIGNATURES] directive, and disabled with the [SKIP_SIGNATURE_DUMP] test directive.
 *
 * Other useful directives:
 * - [MUTE_SIGNATURE_COMPARISON_K2] can be used for muting the comparison result on the K2 frontend. The comparison will
 *   still be performed, and if it succeeds, the test will fail with a message reminding you to unmute it.
 * - [DUMP_LOCAL_DECLARATION_SIGNATURES] enables printing signatures for local functions and classes.
 *
 * Since mangled names and signatures may be different depending on the backend, in order to reduce the number
 * of expectation files, this handler uses `// CHECK` blocks to compare the dump with an expectation in a smarter way than
 * just character by character.
 *
 * For example, the expectation file may look like this:
 *
 * ```kotlin
 * // CHECK JVM_IR:
 * //   Mangled name: #test(){}kotlin.Int
 * //   Public signature: /test|4216975235718029399[0]
 * // CHECK JS_IR NATIVE:
 * //   Mangled name: #test(){}
 * //   Public signature: /test|6620506149988718649[0]
 * fun test(): Int
 * ```
 *
 * Here, on the JVM backend the mangled names include the return value, while on the JS and Native backends
 * this is not the case, so we use two `// CHECK` blocks to reflect that fact.
 *
 * When running a test against some backend, we do the following.
 *
 * 1. In order to compare this expectation with the actual output, we first parse all the `// CHECK` block groups in
 *    the `*.sig.kt.txt` file.
 *
 * 2. While dumping the declarations, for each declaration that we want to print the mangled name and
 *    the signature for, we take the next parsed group, and iterate over `// CHECK` blocks in that group.
 *
 * 3. For each parsed `// CHECK` block the following logic is applied:
 *    - If the current `// CHECK` block includes the backend that we're running this test against, or doesn't include
 *      any backends, we print the actual mangled name and signature for this declaration.
 *    - Otherwise, we print the parsed `// CHECK` block as is.
 */
class IrMangledNameAndSignatureDumpHandler(testServices: TestServices) : IrPrettyKotlinDumpHandler(testServices) {

    companion object {
        const val DUMP_EXTENSION = "sig.kt.txt"
    }

    override fun getDumpExtension(module: TestModule) = DUMP_EXTENSION

    /**
     * Iterates over parsed `// CHECK` block groups in [expectedFile].
     *
     * Each element is a list of `// CHECK` blocks for a single declaration.
     */
    private val checkBlockGroupIterator: Iterator<CheckBlockGroup> by lazy(LazyThreadSafetyMode.NONE) {
        try {
            parseAllCheckBlocks(expectedFile.readText())
        } catch (e: FileNotFoundException) {
            emptyList()
        }.iterator()
    }

    override fun createDumper(module: TestModule, info: IrBackendInput, builder: StringBuilder, printFileName: Boolean): KotlinLikeDumper =
        IrMangledNameAndSignatureDumper(
            Printer(builder, 1, "  "),
            KotlinLikeDumpOptions(
                printFileName = printFileName,
                printFilePath = false,
                printFakeOverridesStrategy = FakeOverridesStrategy.ALL_EXCEPT_ANY,
                printUnitReturnType = true,
            ),
            module,
            info.irMangler,
            info.descriptorMangler,
            info.firMangler,
            info.irModuleFragment.irBuiltins,
        )

    private val isMuted: Boolean
        get() {
            val defaultsProvider = testServices.defaultsProvider
            val ignoredBackends = testServices.moduleStructure.allDirectives[MUTE_SIGNATURE_COMPARISON_K2]
            return defaultsProvider.defaultFrontend == FrontendKinds.FIR &&
                    (defaultsProvider.defaultTargetBackend in ignoredBackends || TargetBackend.ANY in ignoredBackends)
        }

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (DUMP_SIGNATURES !in module.directives || SKIP_SIGNATURE_DUMP in module.directives) return
        dumpModule(module, info)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        try {
            super.processAfterAllModules(someAssertionWasFailed)
            if (isMuted) {
                throw AssertionError("Looks like this test can be unmuted. Please remove the $MUTE_SIGNATURE_COMPARISON_K2 directive.")
            }
        } catch (e: FileComparisonFailure) {
            if (!isMuted) {
                throw e
            }
        }
    }

    /**
     * A customized [KotlinLikeDumper] that prints declarations without their bodies, and also prints their signatures and mangled names.
     */
    private inner class IrMangledNameAndSignatureDumper(
        printer: Printer,
        options: KotlinLikeDumpOptions,
        val module: TestModule,
        val irMangler: KotlinMangler.IrMangler,
        val descriptorMangler: KotlinMangler.DescriptorMangler,
        val firMangler: FirMangler?,
        val irBuiltIns: IrBuiltIns,
    ) : KotlinLikeDumper(printer, options) {

        val targetBackend: TargetBackend
            get() = module.targetBackend!!

        private val IrDeclaration.isFunctionWithNonUnitReturnType: Boolean
            get() = this is IrSimpleFunction && !returnType.isUnit()

        private val IrDeclaration.isMainFunction: Boolean
            get() = isTopLevel && this is IrSimpleFunction && name.asString() == "main"

        private val IrDeclaration.potentiallyHasDifferentMangledNamesDependingOnBackend: Boolean
            get() = isMainFunction ||
                    isFunctionWithNonUnitReturnType ||
                    parent.let { it is IrDeclaration && it.potentiallyHasDifferentMangledNamesDependingOnBackend }

        @OptIn(ObsoleteDescriptorBasedAPI::class)
        private fun Printer.printSignatureAndMangledName(declaration: IrDeclaration) {

            val symbol = declaration.symbol

            val computedMangledNames = mutableListOf<ComputedMangledName>()

            irMangler.addMangledNameTo(computedMangledNames, declaration)
            descriptorMangler.addMangledNameTo(computedMangledNames, symbol.descriptor)
            ((declaration as? IrMetadataSourceOwner)?.metadata as? FirMetadataSource)?.fir?.let {
                firMangler?.addMangledNameTo(
                    computedMangledNames,
                    it
                )
            }

            fun printActualMangledNamesAndSignatures() {
                printMangledNames(computedMangledNames)

                symbol.signature?.let {
                    println("//   Public signature: ${it.render()}")
                }
                symbol.privateSignature?.let {
                    println("//   Private signature: ${it.render()}")
                }
            }

            var printedActualMangledNameAndSignature = false
            if (checkBlockGroupIterator.hasNext()) {
                val checkBlockGroupForDeclaration = checkBlockGroupIterator.next()
                for (checkBlock in checkBlockGroupForDeclaration) {
                    printlnCheckMarker(checkBlock.backends)
                    if (checkBlock.backends.isEmpty() || TargetBackend.ANY in checkBlock.backends || targetBackend in checkBlock.backends) {
                        // If this // CHECK block corresponds to the current target backend, print the actual values
                        printActualMangledNamesAndSignatures()
                        printedActualMangledNameAndSignature = true
                    } else {
                        // Otherwise, print the // CHECK block from the expectation file as is
                        checkBlock.expectations.forEach(this::println)
                    }
                }
                if (!printedActualMangledNameAndSignature) {
                    printlnCheckMarker(listOf(targetBackend))
                    printActualMangledNamesAndSignatures()
                }
            } else {
                if (declaration.potentiallyHasDifferentMangledNamesDependingOnBackend) {
                    // This is a heuristic to print `// CHECK <backend>:` instead of just `// CHECK:` for new declarations
                    // for which the difference between backend-specific manglers is known to take place.
                    // This is purely for convenience when adding a new test.
                    printlnCheckMarker(listOf(targetBackend))
                } else {
                    printlnCheckMarker(emptyList())
                }
                printActualMangledNamesAndSignatures()
            }
        }

        private fun IrSimpleFunction.isHiddenEnumMethod() = allOverridden(includeSelf = true).any {
            it.dispatchReceiverParameter?.type?.classOrNull == irBuiltIns.enumClass && it.name in HIDDEN_ENUM_METHOD_NAMES
        }

        @Suppress("RedundantIf")
        private fun excludeDeclaration(declaration: IrDeclaration): Boolean {
            // Don't print fake overrides of Java fields
            if (declaration is IrProperty &&
                declaration.isFakeOverride &&
                declaration
                    .collectRealOverrides()
                    .all { it.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && it.backingField != null }
            ) {
                return true
            }

            // Don't print certain fake overrides coming from Java classes
            if (declaration is IrSimpleFunction &&
                declaration.isFakeOverride &&
                (declaration.isStatic || declaration.hasPlatformDependent() || declaration.isHiddenEnumMethod())
            ) {
                return true
            }

            return false
        }

        private inline fun <Receiver, Body : IrBody> Receiver?.temporarilyRemoveThenVisit(
            property: KMutableProperty1<Receiver, Body?>,
            data: IrDeclaration?,
            block: () -> Unit,
        ) {
            temporarilySet(property, null) { body ->
                block()
                body
            }?.accept(this@IrMangledNameAndSignatureDumper, data)
        }

        override fun printModifiersWithNoIndent(
            declaration: IrDeclaration,
            visibility: DescriptorVisibility,
            isExpect: Boolean,
            modality: Modality?,
            isExternal: Boolean,
            isOverride: Boolean,
            isFakeOverride: Boolean,
            isLateinit: Boolean,
            isTailrec: Boolean,
            isVararg: Boolean,
            isSuspend: Boolean,
            isInner: Boolean,
            isInline: Boolean,
            isValue: Boolean,
            isData: Boolean,
            isCompanion: Boolean,
            isFunInterface: Boolean,
            classKind: ClassKind?,
            isInfix: Boolean,
            isOperator: Boolean,
            isInterfaceMember: Boolean
        ) {
            super.printModifiersWithNoIndent(
                declaration,
                visibility,
                isExpect,
                modality,
                // K2 doesn't always print 'external' modifier, unlike K1. Unify.
                isExternal || declaration.parents.any { it is IrPossiblyExternalDeclaration && it.isExternal },
                isOverride,
                isFakeOverride,
                isLateinit,
                isTailrec,
                isVararg,
                isSuspend,
                isInner,
                isInline,
                isValue,
                isData,
                isCompanion,
                isFunInterface,
                classKind,
                isInfix,
                isOperator,
                isInterfaceMember
            )
        }

        override fun visitChildDeclarations(declarationContainer: IrDeclarationContainer, data: IrDeclaration?) {
            declarationContainer
                .declarations
                .filterNot(::excludeDeclaration)
                .stableOrdered() // The order of declarations may differ between K1 and K2, so we sort them.
                .forEach { it.accept(this, data) }
        }

        override fun filterAnnotations(annotations: List<IrConstructorCall>) =
            // Remove annotations that may differ between K1 and K2
            super.filterAnnotations(annotations).filter { it.symbol.owner.constructedClass.kotlinFqName !in EXCLUDED_ANNOTATIONS }

        override fun filterSuperTypes(superTypes: List<IrType>): List<IrType> {
            // Use stable order in the inheritance clause (because the actual order may differ between K1 and K2)

            fun isInterfaceType(type: IrType) = type.classifierOrNull?.let {
                it is IrClassSymbol && it.owner.isInterface
            } ?: false

            return super.filterSuperTypes(superTypes)
                .partition(::isInterfaceType)
                .swap() // Interface types in the clause will always go last.
                .toList()
                .map { it.sortedBy(IrType::render) }
                .flatten()
        }

        override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?) {
            p.printSignatureAndMangledName(declaration)
            super.visitDeclaration(declaration, data)
        }

        override fun visitClass(declaration: IrClass, data: IrDeclaration?) {
            p.printSignatureAndMangledName(declaration)
            when {
                declaration.isEnumClass ->
                    // Enum classes are always open on K2, but not on K1. Unify.
                    declaration.temporarilySet(IrClass::modality, Modality.FINAL) {
                        super.visitClass(declaration, data)
                    }
                declaration.isInner && declaration.parent.let { it is IrClass && it.isEnumEntry } ->
                    // Inner classes inside enum entries have local visibility on K2, but not on K1. Unify.
                    declaration.temporarilySet(IrClass::visibility, DescriptorVisibilities.LOCAL) {
                        super.visitClass(declaration, data)
                    }
                else -> super.visitClass(declaration, data)
            }
        }

        override fun visitTypeAlias(declaration: IrTypeAlias, data: IrDeclaration?) {
            p.printSignatureAndMangledName(declaration)
            super.visitTypeAlias(declaration, data)
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction, data: IrDeclaration?) {
            when {
                declaration.isEqualsInheritedFromAny() ->
                    // On K1 the equals() method is marked as `operator fun` for some reason. Unify.
                    declaration.temporarilySet(IrSimpleFunction::isOperator, false) {
                        super.visitSimpleFunction(declaration, data)
                    }
                else -> super.visitSimpleFunction(declaration, data)
            }
        }

        override fun visitConstructor(declaration: IrConstructor, data: IrDeclaration?) {
            p.printSignatureAndMangledName(declaration)
            declaration.temporarilyRemoveThenVisit(IrConstructor::body, null) {
                super.visitConstructor(declaration, data)
            }
        }

        override fun visitEnumEntry(declaration: IrEnumEntry, data: IrDeclaration?) {
            p.printSignatureAndMangledName(declaration)
            declaration.temporarilyRemoveThenVisit(IrEnumEntry::initializerExpression, null) {
                super.visitEnumEntry(declaration, data)
            }
        }

        override fun visitProperty(declaration: IrProperty, data: IrDeclaration?) {
            p.printSignatureAndMangledName(declaration)
            declaration.backingField.temporarilyRemoveThenVisit(IrField::initializer, null) {
                super.visitProperty(declaration, data)
            }
        }

        override fun visitField(declaration: IrField, data: IrDeclaration?) {
            p.printSignatureAndMangledName(declaration)
            declaration.temporarilyRemoveThenVisit(IrField::initializer, null) {
                super.visitField(declaration, data)
            }
        }

        override fun printSimpleFunction(
            function: IrSimpleFunction,
            keyword: String,
            name: String,
            printTypeParametersAndExtensionReceiver: Boolean,
            printSignatureAndBody: Boolean
        ) {
            if (!shouldPrintSimpleFunction(function)) return

            p.printSignatureAndMangledName(function)
            function.temporarilyRemoveThenVisit(IrSimpleFunction::body, null) {
                super.printSimpleFunction(function, keyword, name, printTypeParametersAndExtensionReceiver, printSignatureAndBody)
                p.printlnWithNoIndent()
            }
        }

        override fun visitBody(body: IrBody, data: IrDeclaration?) {
            if (DUMP_LOCAL_DECLARATION_SIGNATURES !in module.directives) return

            // Don't print bodies, but print local declarations declared in those bodies (except variables and local delegated properties)
            body.acceptChildren(
                object : IrElementVisitor<Unit, IrDeclaration?> {
                    override fun visitElement(element: IrElement, data: IrDeclaration?) {
                        element.acceptChildren(this, data)
                    }

                    override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclaration?) {
                        p.println()
                        p.pushIndent()
                        declaration.accept(this@IrMangledNameAndSignatureDumper, data)
                        p.popIndent()
                    }

                    override fun visitVariable(declaration: IrVariable, data: IrDeclaration?) {
                        declaration.acceptChildren(this, data)
                    }

                    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty, data: IrDeclaration?) {
                        declaration.acceptChildren(this, data)
                    }
                },
                data
            )
        }

        override fun visitBlockBody(body: IrBlockBody, data: IrDeclaration?) {
            visitBody(body, data)
        }

        override fun visitExpressionBody(body: IrExpressionBody, data: IrDeclaration?) {
            visitBody(body, data)
        }

        override fun visitSyntheticBody(body: IrSyntheticBody, data: IrDeclaration?) {
            visitBody(body, data)
        }

        override fun printAValueParameterWithNoIndent(valueParameter: IrValueParameter, data: IrDeclaration?) {
            valueParameter.temporarilyRemoveThenVisit(IrValueParameter::defaultValue, data) {
                super.printAValueParameterWithNoIndent(valueParameter, data)
            }
        }

        override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer, data: IrDeclaration?) {
            declaration.body.accept(this, data)
        }
    }
}

/**
 * The annotations that are known not to affect signature computation but that may be generated inconsistently on K1 and K2.
 */
private val EXCLUDED_ANNOTATIONS = setOf(
    StandardNames.FqNames.deprecated,
    StandardNames.FqNames.unsafeVariance,
    StandardNames.FqNames.suppress,
    StandardNames.FqNames.parameterName,
    JvmSymbols.FLEXIBLE_MUTABILITY_ANNOTATION_FQ_NAME,
    JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME,
    JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME,
    StandardClassIds.Annotations.ContextFunctionTypeParams.asSingleFqName(),
    JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION,
    JvmAnnotationNames.JETBRAINS_NULLABLE_ANNOTATION,
    intrinsicConstEvaluationAnnotation,
)

private val HIDDEN_ENUM_METHOD_NAMES = setOf(Name.identifier("finalize"), Name.identifier("getDeclaringClass"))

private inline fun <Receiver, PropertyValue, R> Receiver?.temporarilySet(
    property: KMutableProperty1<Receiver, PropertyValue>,
    temporaryValue: PropertyValue,
    block: (PropertyValue?) -> R,
): R {
    if (this == null) return block(null)
    val originalValue = property.get(this)
    property.set(this, temporaryValue)
    try {
        return block(originalValue)
    } finally {
        property.set(this, originalValue)
    }
}

private data class ComputedMangledName(
    val manglerName: String,
    val compatibleMode: Boolean,
    val value: String,
)

/**
 * For each pair in the list, pads its first element with a number of spaces and concatenates it with the second element in such a way
 * that the second elements of each pair are vertically aligned in concatenated strings.
 *
 * In other words, transforms this list:
 *
 * ```kotlin
 * listOf(
 *   Pair("Capacity: ", "normal"),
 *   Pair("Usability: ", "very good"),
 *   Pair("Magic: ", "minimal"),
 * )
 * ```
 *
 * to this:
 *
 * ```kotlin
 * listOf(
 *   "Capacity:  normal",
 *   "Usability: very good",
 *   "Magic:     minimal",
 * )
 * ```
 */
private fun Iterable<Pair<String, String>>.aligned(): List<String> {
    val maxPrefixLength = maxOf { it.first.length }
    return map { (prefix, suffix) -> prefix.padEnd(maxPrefixLength) + suffix }
}

private inline fun Iterable<ComputedMangledName>.printAligned(printer: Printer, prefix: (ComputedMangledName) -> String) {
    map { prefix(it) to it.value }.aligned().forEach(printer::println)
}

private fun Printer.printMangledNames(computedMangledNames: List<ComputedMangledName>) {
    val distinctNames = computedMangledNames.distinctBy { it.value }

    // If mangled names computed from all three representations (descriptors, IR, FIR) and modes match,
    // print just one mangled name.
    distinctNames.singleOrNull()?.let {
        println("//   Mangled name: ${it.value}")
        return
    }

    if (distinctNames.same { it.compatibleMode }) {
        // If the mangled names differ only by the mangler used (but not the compatibility mode), print
        // only mangled names from each mangler.
        computedMangledNames
            .distinctBy { it.manglerName }
            .printAligned(this) { "//   Mangled name computed from ${it.manglerName}: " }
    } else if (distinctNames.same { it.manglerName }) {
        // If the mangled names differ only by the compatibility mode used (but not the mangler), print
        // only mangled names for each compatibility mode.
        computedMangledNames
            .distinctBy { it.compatibleMode }
            .printAligned(this) { "//   Mangled name (compatible mode: ${it.compatibleMode}): " }
    } else {
        // Otherwise, print the whole matrix.
        computedMangledNames
            .printAligned(this) { "//   Mangled name computed from ${it.manglerName} (compatible mode: ${it.compatibleMode}): " }
    }
}

private fun Printer.printlnCheckMarker(backends: List<TargetBackend>) {
    print(CHECK_MARKER)
    for (backend in backends) {
        printWithNoIndent(" ")
        printWithNoIndent(backend.name)
    }
    printlnWithNoIndent(":")
}

private fun <Declaration, Mangler : KotlinMangler<Declaration>> Mangler.addMangledNameTo(
    collector: MutableList<ComputedMangledName>,
    declaration: Declaration
) {
    listOf(false, true).mapTo(collector) { compatibleMode ->
        val mangledName = try {
            declaration.mangleString(compatibleMode)
        } catch (e: Throwable) {
            // Kotlin-like IR renderer suppresses exceptions thrown during rendering, which leads to missing renders that are hard to debug.
            // Because this routine is executed during rendering, we print the exception description instead of a proper mangled name.
            val message = e.toString()
            buildString {
                append("could not compute mangled name: ")
                when (val newlineIndex = message.indexOf('\n')) {
                    -1 -> append(message)
                    else -> {
                        append(message.substring(0, newlineIndex))
                        append("... (truncated)")
                    }
                }
            }
        }
        ComputedMangledName(manglerName, compatibleMode, mangledName)
    }
}

/**
 * Represents a single `// CHECK` block.
 *
 * @property backends The backends this `// CHECK` block is for.
 * @property expectations The list of expectation lines in this `// CHECK` block.
 */
private data class CheckBlock(val backends: List<TargetBackend>, val expectations: List<String>)

/**
 * A list of `// CHECK` blocks for a single declaration.
 */
private typealias CheckBlockGroup = List<CheckBlock>

private fun parseAllCheckBlocks(input: String): List<CheckBlockGroup> {
    val result = mutableListOf<CheckBlockGroup>()
    val lineIterator = input.lines().iterator()

    var currentCheckBlockGroup = mutableListOf<CheckBlock>() // CHECK blocks for a single declaration

    var line = if (lineIterator.hasNext()) lineIterator.next() else return emptyList()

    while (true) {
        val trimmed = line.trim()
        if (trimmed.startsWith(CHECK_MARKER)) {
            val (nextCheckMarker, checkBlock) = parseSingleCheckBlock(trimmed, lineIterator)
            currentCheckBlockGroup.add(checkBlock)
            if (nextCheckMarker != null) {
                line = nextCheckMarker
                continue
            }

            result.add(currentCheckBlockGroup)
        }
        if (lineIterator.hasNext()) {
            line = lineIterator.next()
            currentCheckBlockGroup = mutableListOf()
        } else {
            break
        }
    }
    return result
}

private val whitespaceRegex = "\\s+".toRegex()

/**
 * Parses a single check block.
 *
 * The valid `// CHECK` block is multiline string that starts with the `// CHECK` comment optionally followed by a whitespace-separated list
 * of [TargetBackend] names, and ending with a colon. After that, an arbitrary number of single-line comments may follow. The `// CHECK` block
 * ends with the first non-comment line.
 *
 * For example, this text
 * ```kotlin
 * // CHECK JS_IR NATIVE:
 * //   Mangled name: #test(){}
 * //   Public signature: /test|6620506149988718649[0]
 * fun test(): Int {
 *   return 42
 * }
 * ```
 * will be parsed into:
 * ```kotlin
 * CheckBlock(
 *   backends = listOf(TargetBackend.JS_IR, TargetBackend.NATIVE),
 *   expectations = listOf(
 *     "//   Mangled name: #test(){}",
 *     "//   Public signature: /test|6620506149988718649[0]"
 *   )
 * )
 * ```
 *
 * @param trimmedCheckLine The line that starts with `// CHECK <BACKEND>*:`
 * @param lineIterator The iterator over lines in the expectation file.
 * @return The line representing the beginning of the next `// CHECK` block (if there is one), and the parsed `// CHECK` block.
 */
private fun parseSingleCheckBlock(trimmedCheckLine: String, lineIterator: Iterator<String>): Pair<String?, CheckBlock> {
    assert(trimmedCheckLine.startsWith(CHECK_MARKER))
    val colonIndex = trimmedCheckLine.indexOf(':')
    if (colonIndex < 0) {
        error("Expected colon after '$CHECK_MARKER' in '$trimmedCheckLine'")
    }
    val backends = trimmedCheckLine
        .substring(CHECK_MARKER.length, colonIndex)
        .splitToSequence(whitespaceRegex)
        .filter { it.isNotEmpty() }
        .map { enumValueOf<TargetBackend>(it) }
        .toList()
    val expectations = mutableListOf<String>()
    for (line in lineIterator) {
        val trimmed = line.trim()
        if (trimmed.startsWith(CHECK_MARKER)) {
            // Encountered the next // CHECK block
            return trimmed to CheckBlock(backends, expectations)
        }
        if (trimmed.startsWith("//")) {
            expectations.add(trimmed)
        } else {
            // The // CHECK block has ended, no more // CHECK blocks ahead in this group.
            break
        }
    }
    // Either we have no more lines, or the next line is not valid beginning of a `// CHECK` block.
    return null to CheckBlock(backends, expectations)
}
