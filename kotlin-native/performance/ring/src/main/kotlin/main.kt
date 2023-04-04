/*
 * Copyright 2010-2017 JetBrains s.r.o.
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


import org.jetbrains.ring.*
import octoTest
import org.jetbrains.benchmarksLauncher.*
import kotlinx.cli.*

class RingLauncher : Launcher() {
    override val baseBenchmarksSet =
            mutableMapOf(
                    "AbstractMethod.sortStrings" to BenchmarkEntryWithInit.create(::AbstractMethodBenchmark, { sortStrings() }),
                    "ClassArray.copy" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { copy() }),
                    "ClassArray.filter" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { filter() }),
                    "ClassArray.countFiltered" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { countFiltered() }),
                    "ClassBaseline.consume" to BenchmarkEntryWithInit.create(::ClassBaselineBenchmark, { consume() }),
                    "ClassBaseline.consumeField" to BenchmarkEntryWithInit.create(::ClassBaselineBenchmark, { consumeField() }),
                    "ClassBaseline.allocateListAndFill" to BenchmarkEntryWithInit.create(::ClassBaselineBenchmark, { allocateListAndFill() }),
                    "ClassList.copy" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { copy() }),
                    "ClassList.mapWithLambda" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { mapWithLambda() }),
                    "ClassList.filter" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { filter() }),
                    "ClassList.reduce" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { reduce() }),
                    "ClassStream.copy" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { copy() }),
                    "ClassStream.filter" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { filter() }),
                    "ClassStream.reduce" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { reduce() }),
                    "DeltaBlue" to BenchmarkEntryWithInit.create(::DeltaBlueBenchmark, { deltaBlue() }),
                    "Elvis.testElvis" to BenchmarkEntryWithInit.create(::ElvisBenchmark, { testElvis() }),
                    "Euler.problem1bySequence" to BenchmarkEntryWithInit.create(::EulerBenchmark, { problem1bySequence() }),
                    "Euler.problem9" to BenchmarkEntryWithInit.create(::EulerBenchmark, { problem9() }),
                    "Euler.problem14" to BenchmarkEntryWithInit.create(::EulerBenchmark, { problem14() }),
                    "Fibonacci.calcClassic" to BenchmarkEntryWithInit.create(::FibonacciBenchmark, { calcClassic() }),
                    "Fibonacci.calc" to BenchmarkEntryWithInit.create(::FibonacciBenchmark, { calc() }),
                    "ForLoops.arrayLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { arrayLoop() }),
                    "ForLoops.arrayIndicesLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { arrayIndicesLoop() }),
                    "Inline.calculateInline" to BenchmarkEntryWithInit.create(::InlineBenchmark, { calculateInline() }),
                    "Lambda.noncapturingLambda" to BenchmarkEntryWithInit.create(::LambdaBenchmark, { noncapturingLambda() }),
                    "Lambda.capturingLambda" to BenchmarkEntryWithInit.create(::LambdaBenchmark, { capturingLambda() }),
                    "Lambda.methodReference" to BenchmarkEntryWithInit.create(::LambdaBenchmark, { methodReference() }),
                    "Loop.arrayLoop" to BenchmarkEntryWithInit.create(::LoopBenchmark, { arrayLoop() }),
                    "Loop.rangeLoop" to BenchmarkEntryWithInit.create(::LoopBenchmark, { rangeLoop() }),
                    "Loop.arrayWhileLoop" to BenchmarkEntryWithInit.create(::LoopBenchmark, { arrayWhileLoop() }),
                    "Loop.arrayForeachLoop" to BenchmarkEntryWithInit.create(::LoopBenchmark, { arrayForeachLoop() }),
                    "Loop.arrayListForeachLoop" to BenchmarkEntryWithInit.create(::LoopBenchmark, { arrayListForeachLoop() }),
                    "MatrixMap.add" to BenchmarkEntryWithInit.create(::MatrixMapBenchmark, { add() }),
                    "PrimeList.calcDirect" to BenchmarkEntryWithInit.create(::PrimeListBenchmark, { calcDirect() }),
                    "PrimeList.calcEratosthenes" to BenchmarkEntryWithInit.create(::PrimeListBenchmark, { calcEratosthenes() }),
                    "Richards" to BenchmarkEntryWithInit.create(::RichardsBenchmark, { runRichards() }),
                    "Singleton.access" to BenchmarkEntryWithInit.create(::SingletonBenchmark, { access() }),
                    "Splay" to BenchmarkEntryWithInitAndValidation.create(::SplayBenchmark, { runSplay() }, { splayTearDown() }),
                    "SplayWithWorkers" to BenchmarkEntryWithInitAndValidation.create(::SplayBenchmarkUsingWorkers, { runSplayWorkers() }, { splayTearDownWorkers() }),
                    "SplayParallel" to BenchmarkEntryWithInitAndValidation.create(::SplayBenchmarkParallel, { runSplayParallel() }, { splayTearDownParallel() }),
                    "String.stringConcat" to BenchmarkEntryWithInit.create(::StringBenchmark, { stringConcat() }),
                    "String.stringBuilderConcat" to BenchmarkEntryWithInit.create(::StringBenchmark, { stringBuilderConcat() }),
                    "String.stringBuilderConcatNullable" to BenchmarkEntryWithInit.create(::StringBenchmark, { stringBuilderConcatNullable() }),
                    "String.summarizeSplittedCsv" to BenchmarkEntryWithInit.create(::StringBenchmark, { summarizeSplittedCsv() }),
                    "Switch.testStringsSwitch" to BenchmarkEntryWithInit.create(::SwitchBenchmark, { testStringsSwitch() }),
                    "Switch.testEnumsSwitch" to BenchmarkEntryWithInit.create(::SwitchBenchmark, { testEnumsSwitch() }),
                    "Switch.testSealedWhenSwitch" to BenchmarkEntryWithInit.create(::SwitchBenchmark, { testSealedWhenSwitch() }),
                    "WithIndicies.withIndicies" to BenchmarkEntryWithInit.create(::WithIndiciesBenchmark, { withIndicies() }),
                    "OctoTest" to BenchmarkEntry(::octoTest),
                    "Calls.finalMethod" to BenchmarkEntryWithInit.create(::CallsBenchmark, { finalMethodCall() }),
                    "Calls.openMethodMonomorphic" to BenchmarkEntryWithInit.create(::CallsBenchmark, { classOpenMethodCall_MonomorphicCallsite() }),
                    "Calls.interfaceMethodMonomorphic" to BenchmarkEntryWithInit.create(::CallsBenchmark, { interfaceMethodCall_MonomorphicCallsite() }),
                    "Calls.returnBoxUnboxFolding" to BenchmarkEntryWithInit.create(::CallsBenchmark, { returnBoxUnboxFolding() }),
                    "Calls.parameterBoxUnboxFolding" to BenchmarkEntryWithInit.create(::CallsBenchmark, { parameterBoxUnboxFolding() }),
                    "CoordinatesSolver.solve" to BenchmarkEntryWithInit.create(::CoordinatesSolverBenchmark, { solve() }),
                    "GraphSolver.solve" to BenchmarkEntryWithInit.create(::GraphSolverBenchmark, { solve() }),
                    "Casts.classCast" to BenchmarkEntryWithInit.create(::CastsBenchmark, { classCast() }),
                    "Casts.interfaceCast" to BenchmarkEntryWithInit.create(::CastsBenchmark, { interfaceCast() }),
                    "LinkedListWithAtomicsBenchmark" to BenchmarkEntryWithInit.create(::LinkedListWithAtomicsBenchmark, { ensureNext() }),
                    "Inheritance.baseCalls" to BenchmarkEntryWithInit.create(::InheritanceBenchmark, { baseCalls() }),
                    "ChainableBenchmark.testChainable" to BenchmarkEntryWithInit.create(::ChainableBenchmark, { testChainable() }),
                    "BunnymarkBenchmark.testBunnymark" to BenchmarkEntryWithInit.create(::BunnymarkBenchmark, { testBunnymark() }),
                    "ArrayCopyBenchmark.copyInSameArray" to BenchmarkEntryWithInit.create(::ArrayCopyBenchmark, { copyInSameArray() })
            )


    override val extendedBenchmarksSet: MutableMap<String, AbstractBenchmarkEntry> =
            mutableMapOf(
                    "AbstractMethod.sortStringsWithComparator" to BenchmarkEntryWithInit.create(::AbstractMethodBenchmark, { sortStringsWithComparator() }),
                    "AllocationBenchmark.allocateObjects" to BenchmarkEntryWithInit.create(::AllocationBenchmark, { allocateObjects() }),
                    "ClassArray.copyManual" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { copyManual() }),
                    "ClassArray.filterAndCount" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { filterAndCount() }),
                    "ClassArray.filterAndMap" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { filterAndMap() }),
                    "ClassArray.filterAndMapManual" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { filterAndMapManual() }),
                    "ClassArray.filterManual" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { filterManual() }),
                    "ClassArray.countFilteredManual" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { countFilteredManual() }),
                    "ClassArray.countFilteredLocal" to BenchmarkEntryWithInit.create(::ClassArrayBenchmark, { countFilteredLocal() }),
                    "ClassBaseline.allocateList" to BenchmarkEntryWithInit.create(::ClassBaselineBenchmark, { allocateList() }),
                    "ClassBaseline.allocateArray" to BenchmarkEntryWithInit.create(::ClassBaselineBenchmark, { allocateArray() }),
                    "ClassBaseline.allocateListAndWrite" to BenchmarkEntryWithInit.create(::ClassBaselineBenchmark, { allocateListAndWrite() }),
                    "ClassBaseline.allocateArrayAndFill" to BenchmarkEntryWithInit.create(::ClassBaselineBenchmark, { allocateArrayAndFill() }),
                    "ClassList.copyManual" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { copyManual() }),
                    "ClassList.filterAndCount" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { filterAndCount() }),
                    "ClassList.filterAndCountWithLambda" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { filterAndCountWithLambda() }),
                    "ClassList.filterWithLambda" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { filterWithLambda() }),
                    "ClassList.countWithLambda" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { countWithLambda() }),
                    "ClassList.filterAndMapWithLambda" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { filterAndMapWithLambda() }),
                    "ClassList.filterAndMapWithLambdaAsSequence" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { filterAndMapWithLambdaAsSequence() }),
                    "ClassList.filterAndMap" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { filterAndMap() }),
                    "ClassList.filterAndMapManual" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { filterAndMapManual() }),
                    "ClassList.filterManual" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { filterManual() }),
                    "ClassList.countFilteredManual" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { countFilteredManual() }),
                    "ClassList.countFiltered" to BenchmarkEntryWithInit.create(::ClassListBenchmark, { countFiltered() }),
                    "ClassStream.copyManual" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { copyManual() }),
                    "ClassStream.filterAndCount" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { filterAndCount() }),
                    "ClassStream.filterAndMap" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { filterAndMap() }),
                    "ClassStream.filterAndMapManual" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { filterAndMapManual() }),
                    "ClassStream.filterManual" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { filterManual() }),
                    "ClassStream.countFilteredManual" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { countFilteredManual() }),
                    "ClassStream.countFiltered" to BenchmarkEntryWithInit.create(::ClassStreamBenchmark, { countFiltered() }),
                    "CompanionObject.invokeRegularFunction" to BenchmarkEntryWithInit.create(::CompanionObjectBenchmark, { invokeRegularFunction() }),
                    "DefaultArgument.testOneOfTwo" to BenchmarkEntryWithInit.create(::DefaultArgumentBenchmark, { testOneOfTwo() }),
                    "DefaultArgument.testTwoOfTwo" to BenchmarkEntryWithInit.create(::DefaultArgumentBenchmark, { testTwoOfTwo() }),
                    "DefaultArgument.testOneOfFour" to BenchmarkEntryWithInit.create(::DefaultArgumentBenchmark, { testOneOfFour() }),
                    "DefaultArgument.testFourOfFour" to BenchmarkEntryWithInit.create(::DefaultArgumentBenchmark, { testFourOfFour() }),
                    "DefaultArgument.testOneOfEight" to BenchmarkEntryWithInit.create(::DefaultArgumentBenchmark, { testOneOfEight() }),
                    "DefaultArgument.testEightOfEight" to BenchmarkEntryWithInit.create(::DefaultArgumentBenchmark, { testEightOfEight() }),
                    "Elvis.testCompositeElvis" to BenchmarkEntryWithInit.create(::ElvisBenchmark, { testCompositeElvis() }),
                    "Euler.problem1" to BenchmarkEntryWithInit.create(::EulerBenchmark, { problem1() }),
                    "Euler.problem2" to BenchmarkEntryWithInit.create(::EulerBenchmark, { problem2() }),
                    "Euler.problem4" to BenchmarkEntryWithInit.create(::EulerBenchmark, { problem4() }),
                    "Euler.problem8" to BenchmarkEntryWithInit.create(::EulerBenchmark, { problem8() }),
                    "Euler.problem14full" to BenchmarkEntryWithInit.create(::EulerBenchmark, { problem14full() }),
                    "Fibonacci.calcWithProgression" to BenchmarkEntryWithInit.create(::FibonacciBenchmark, { calcWithProgression() }),
                    "Fibonacci.calcSquare" to BenchmarkEntryWithInit.create(::FibonacciBenchmark, { calcSquare() }),
                    "ForLoops.intArrayLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { intArrayLoop() }),
                    "ForLoops.floatArrayLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { floatArrayLoop() }),
                    "ForLoops.charArrayLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { charArrayLoop() }),
                    "ForLoops.stringLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { stringLoop() }),
                    "ForLoops.uIntArrayLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { uIntArrayLoop() }),
                    "ForLoops.uShortArrayLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { uShortArrayLoop() }),
                    "ForLoops.uLongArrayLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { uLongArrayLoop() }),
                    "ForLoops.intArrayIndicesLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { intArrayIndicesLoop() }),
                    "ForLoops.floatArrayIndicesLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { floatArrayIndicesLoop() }),
                    "ForLoops.charArrayIndicesLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { charArrayIndicesLoop() }),
                    "ForLoops.stringIndicesLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { stringIndicesLoop() }),
                    "ForLoops.uIntArrayIndicesLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { uIntArrayIndicesLoop() }),
                    "ForLoops.uShortArrayIndicesLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { uShortArrayIndicesLoop() }),
                    "ForLoops.uLongArrayIndicesLoop" to BenchmarkEntryWithInit.create(::ForLoopsBenchmark, { uLongArrayIndicesLoop() }),
                    "Inline.calculate" to BenchmarkEntryWithInit.create(::InlineBenchmark, { calculate() }),
                    "Inline.calculateGeneric" to BenchmarkEntryWithInit.create(::InlineBenchmark, { calculateGeneric() }),
                    "Inline.calculateGenericInline" to BenchmarkEntryWithInit.create(::InlineBenchmark, { calculateGenericInline() }),
                    "IntArray.copy" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { copy() }),
                    "IntArray.copyManual" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { copyManual() }),
                    "IntArray.filterAndCount" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { filterAndCount() }),
                    "IntArray.filterSomeAndCount" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { filterSomeAndCount() }),
                    "IntArray.filterAndMap" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { filterAndMap() }),
                    "IntArray.filterAndMapManual" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { filterAndMapManual() }),
                    "IntArray.filter" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { filter() }),
                    "IntArray.filterSome" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { filterSome() }),
                    "IntArray.filterPrime" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { filterPrime() }),
                    "IntArray.filterManual" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { filterManual() }),
                    "IntArray.filterSomeManual" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { filterSomeManual() }),
                    "IntArray.countFilteredManual" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { countFilteredManual() }),
                    "IntArray.countFilteredSomeManual" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { countFilteredSomeManual() }),
                    "IntArray.countFilteredPrimeManual" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { countFilteredPrimeManual() }),
                    "IntArray.countFiltered" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { countFiltered() }),
                    "IntArray.countFilteredSome" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { countFilteredSome() }),
                    "IntArray.countFilteredPrime" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { countFilteredPrime() }),
                    "IntArray.countFilteredLocal" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { countFilteredLocal() }),
                    "IntArray.countFilteredSomeLocal" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { countFilteredSomeLocal() }),
                    "IntArray.reduce" to BenchmarkEntryWithInit.create(::IntArrayBenchmark, { reduce() }),
                    "IntBaseline.consume" to BenchmarkEntryWithInit.create(::IntBaselineBenchmark, { consume() }),
                    "IntBaseline.allocateList" to BenchmarkEntryWithInit.create(::IntBaselineBenchmark, { allocateList() }),
                    "IntBaseline.allocateArray" to BenchmarkEntryWithInit.create(::IntBaselineBenchmark, { allocateArray() }),
                    "IntBaseline.allocateListAndFill" to BenchmarkEntryWithInit.create(::IntBaselineBenchmark, { allocateListAndFill() }),
                    "IntBaseline.allocateArrayAndFill" to BenchmarkEntryWithInit.create(::IntBaselineBenchmark, { allocateArrayAndFill() }),
                    "IntList.copy" to BenchmarkEntryWithInit.create(::IntListBenchmark, { copy() }),
                    "IntList.copyManual" to BenchmarkEntryWithInit.create(::IntListBenchmark, { copyManual() }),
                    "IntList.filterAndCount" to BenchmarkEntryWithInit.create(::IntListBenchmark, { filterAndCount() }),
                    "IntList.filterAndMap" to BenchmarkEntryWithInit.create(::IntListBenchmark, { filterAndMap() }),
                    "IntList.filterAndMapManual" to BenchmarkEntryWithInit.create(::IntListBenchmark, { filterAndMapManual() }),
                    "IntList.filter" to BenchmarkEntryWithInit.create(::IntListBenchmark, { filter() }),
                    "IntList.filterManual" to BenchmarkEntryWithInit.create(::IntListBenchmark, { filterManual() }),
                    "IntList.countFilteredManual" to BenchmarkEntryWithInit.create(::IntListBenchmark, { countFilteredManual() }),
                    "IntList.countFiltered" to BenchmarkEntryWithInit.create(::IntListBenchmark, { countFiltered() }),
                    "IntList.countFilteredLocal" to BenchmarkEntryWithInit.create(::IntListBenchmark, { countFilteredLocal() }),
                    "IntList.reduce" to BenchmarkEntryWithInit.create(::IntListBenchmark, { reduce() }),
                    "IntStream.copy" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { copy() }),
                    "IntStream.copyManual" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { copyManual() }),
                    "IntStream.filterAndCount" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { filterAndCount() }),
                    "IntStream.filterAndMap" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { filterAndMap() }),
                    "IntStream.filterAndMapManual" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { filterAndMapManual() }),
                    "IntStream.filter" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { filter() }),
                    "IntStream.filterManual" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { filterManual() }),
                    "IntStream.countFilteredManual" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { countFilteredManual() }),
                    "IntStream.countFiltered" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { countFiltered() }),
                    "IntStream.countFilteredLocal" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { countFilteredLocal() }),
                    "IntStream.reduce" to BenchmarkEntryWithInit.create(::IntStreamBenchmark, { reduce() }),
                    "Lambda.noncapturingLambdaNoInline" to BenchmarkEntryWithInit.create(::LambdaBenchmark, { noncapturingLambdaNoInline() }),
                    "Lambda.capturingLambdaNoInline" to BenchmarkEntryWithInit.create(::LambdaBenchmark, { capturingLambdaNoInline() }),
                    "Lambda.mutatingLambda" to BenchmarkEntryWithInit.create(::LambdaBenchmark, { mutatingLambda() }),
                    "Lambda.mutatingLambdaNoInline" to BenchmarkEntryWithInit.create(::LambdaBenchmark, { mutatingLambdaNoInline() }),
                    "Lambda.methodReferenceNoInline" to BenchmarkEntryWithInit.create(::LambdaBenchmark, { methodReferenceNoInline() }),
                    "Life" to BenchmarkEntryWithInit.create(::LifeBenchmark, { bench() }),
                    "LifeWithWorkers" to BenchmarkEntryWithInitAndValidation.create(::LifeWithWorkersBenchmark, { benchWithWorkers() }, { terminate() }),
                    "Loop.arrayIndexLoop" to BenchmarkEntryWithInit.create(::LoopBenchmark, { arrayIndexLoop() }),
                    "Loop.arrayListLoop" to BenchmarkEntryWithInit.create(::LoopBenchmark, { arrayListLoop() }),
                    "ParameterNotNull.invokeOneArgWithNullCheck" to BenchmarkEntryWithInit.create(::ParameterNotNullAssertionBenchmark, { invokeOneArgWithNullCheck() }),
                    "ParameterNotNull.invokeOneArgWithoutNullCheck" to BenchmarkEntryWithInit.create(::ParameterNotNullAssertionBenchmark, { invokeOneArgWithoutNullCheck() }),
                    "ParameterNotNull.invokeTwoArgsWithNullCheck" to BenchmarkEntryWithInit.create(::ParameterNotNullAssertionBenchmark, { invokeTwoArgsWithNullCheck() }),
                    "ParameterNotNull.invokeTwoArgsWithoutNullCheck" to BenchmarkEntryWithInit.create(::ParameterNotNullAssertionBenchmark, { invokeTwoArgsWithoutNullCheck() }),
                    "ParameterNotNull.invokeEightArgsWithNullCheck" to BenchmarkEntryWithInit.create(::ParameterNotNullAssertionBenchmark, { invokeEightArgsWithNullCheck() }),
                    "ParameterNotNull.invokeEightArgsWithoutNullCheck" to BenchmarkEntryWithInit.create(::ParameterNotNullAssertionBenchmark, { invokeEightArgsWithoutNullCheck() }),
                    "String.stringConcatNullable" to BenchmarkEntryWithInit.create(::StringBenchmark, { stringConcatNullable() }),
                    "Switch.testSparseIntSwitch" to BenchmarkEntryWithInit.create(::SwitchBenchmark, { testSparseIntSwitch() }),
                    "Switch.testDenseIntSwitch" to BenchmarkEntryWithInit.create(::SwitchBenchmark, { testDenseIntSwitch() }),
                    "Switch.testConstSwitch" to BenchmarkEntryWithInit.create(::SwitchBenchmark, { testConstSwitch() }),
                    "Switch.testObjConstSwitch" to BenchmarkEntryWithInit.create(::SwitchBenchmark, { testObjConstSwitch() }),
                    "Switch.testVarSwitch" to BenchmarkEntryWithInit.create(::SwitchBenchmark, { testVarSwitch() }),
                    "Switch.testDenseEnumsSwitch" to BenchmarkEntryWithInit.create(::SwitchBenchmark, { testDenseEnumsSwitch() }),
                    "WithIndicies.withIndiciesManual" to BenchmarkEntryWithInit.create(::WithIndiciesBenchmark, { withIndiciesManual() }),
                    "Calls.openMethodBimorphic" to BenchmarkEntryWithInit.create(::CallsBenchmark, { classOpenMethodCall_BimorphicCallsite() }),
                    "Calls.openMethodTrimorphic" to BenchmarkEntryWithInit.create(::CallsBenchmark, { classOpenMethodCall_TrimorphicCallsite() }),
                    "Calls.interfaceMethodBimorphic" to BenchmarkEntryWithInit.create(::CallsBenchmark, { interfaceMethodCall_BimorphicCallsite() }),
                    "Calls.interfaceMethodTrimorphic" to BenchmarkEntryWithInit.create(::CallsBenchmark, { interfaceMethodCall_TrimorphicCallsite() }),
                    "Calls.interfaceMethodHexamorphic" to BenchmarkEntryWithInit.create(::CallsBenchmark, { interfaceMethodCall_HexamorphicCallsite() }),
                    "LocalObjects.localArray" to BenchmarkEntryWithInit.create(::LocalObjectsBenchmark, { localArray() }),
                    "ComplexArrays.outerProduct" to BenchmarkEntryWithInit.create(::ComplexArraysBenchmark, { outerProduct() }),
                    "GenericArrayView.origin" to BenchmarkEntryWithInit.create(::GenericArrayViewBenchmark, { origin() }),
                    "GenericArrayView.inlined" to BenchmarkEntryWithInit.create(::GenericArrayViewBenchmark, { inlined() }),
                    "GenericArrayView.specialized" to BenchmarkEntryWithInit.create(::GenericArrayViewBenchmark, { specialized() }),
                    "GenericArrayView.manual" to BenchmarkEntryWithInit.create(::GenericArrayViewBenchmark, { manual() }),
                    "WeakRefBenchmark.aliveReference" to BenchmarkEntryWithInit.create(::WeakRefBenchmark, { aliveReference() }),
                    "WeakRefBenchmark.deadReference" to BenchmarkEntryWithInit.create(::WeakRefBenchmark, { deadReference() }),
                    "WeakRefBenchmark.dyingReference" to BenchmarkEntryWithInit.create(::WeakRefBenchmark, { dyingReference() }),
            )

    init {
        @OptIn(kotlin.ExperimentalStdlibApi::class)
        if (!isExperimentalMM()) {
            baseBenchmarksSet -= listOf("SplayWithWorkers")
        }
    }
}

fun main(args: Array<String>) {
    val launcher = RingLauncher()
    BenchmarksRunner.runBenchmarks(args, { arguments: BenchmarkArguments ->
        if (arguments is BaseBenchmarkArguments) {
            launcher.launch(arguments.warmup, arguments.repeat, arguments.prefix,
                    arguments.filter, arguments.filterRegex, arguments.verbose)
        } else emptyList()
    }, benchmarksListAction = launcher::benchmarksListAction)
}
