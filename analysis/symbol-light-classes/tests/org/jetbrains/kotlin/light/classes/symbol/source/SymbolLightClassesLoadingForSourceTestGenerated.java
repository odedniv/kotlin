/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.source;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/asJava/ultraLightClasses")
@TestDataPath("$PROJECT_ROOT")
public class SymbolLightClassesLoadingForSourceTestGenerated extends AbstractSymbolLightClassesLoadingForSourceTest {
    @Test
    public void testAllFilesPresentInUltraLightClasses() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/asJava/ultraLightClasses"), Pattern.compile("^(.+)\\.(kt|kts)$"), null, true);
    }

    @Test
    @TestMetadata("annotationWithSetParamPropertyModifier.kt")
    public void testAnnotationWithSetParamPropertyModifier() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/annotationWithSetParamPropertyModifier.kt");
    }

    @Test
    @TestMetadata("annotations.kt")
    public void testAnnotations() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/annotations.kt");
    }

    @Test
    @TestMetadata("classModifiers.kt")
    public void testClassModifiers() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/classModifiers.kt");
    }

    @Test
    @TestMetadata("constructors.kt")
    public void testConstructors() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/constructors.kt");
    }

    @Test
    @TestMetadata("coroutines.kt")
    public void testCoroutines() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/coroutines.kt");
    }

    @Test
    @TestMetadata("dataClasses.kt")
    public void testDataClasses() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/dataClasses.kt");
    }

    @Test
    @TestMetadata("defaultMethodInKotlinWithSettingAll.kt")
    public void testDefaultMethodInKotlinWithSettingAll() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/defaultMethodInKotlinWithSettingAll.kt");
    }

    @Test
    @TestMetadata("defaultMethodInKotlinWithSettingAllCompatibility.kt")
    public void testDefaultMethodInKotlinWithSettingAllCompatibility() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/defaultMethodInKotlinWithSettingAllCompatibility.kt");
    }

    @Test
    @TestMetadata("delegatesWithAnnotations.kt")
    public void testDelegatesWithAnnotations() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/delegatesWithAnnotations.kt");
    }

    @Test
    @TestMetadata("delegatingToInterfaces.kt")
    public void testDelegatingToInterfaces() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/delegatingToInterfaces.kt");
    }

    @Test
    @TestMetadata("dollarsInNameLocal.kt")
    public void testDollarsInNameLocal() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/dollarsInNameLocal.kt");
    }

    @Test
    @TestMetadata("enums.kt")
    public void testEnums() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/enums.kt");
    }

    @Test
    @TestMetadata("generics.kt")
    public void testGenerics() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/generics.kt");
    }

    @Test
    @TestMetadata("implementingKotlinCollections.kt")
    public void testImplementingKotlinCollections() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/implementingKotlinCollections.kt");
    }

    @Test
    @TestMetadata("importAliases.kt")
    public void testImportAliases() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/importAliases.kt");
    }

    @Test
    @TestMetadata("inferringAnonymousObjectTypes.kt")
    public void testInferringAnonymousObjectTypes() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/inferringAnonymousObjectTypes.kt");
    }

    @Test
    @TestMetadata("inheritance.kt")
    public void testInheritance() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/inheritance.kt");
    }

    @Test
    @TestMetadata("inlineClasses.kt")
    public void testInlineClasses() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/inlineClasses.kt");
    }

    @Test
    @TestMetadata("inlineOnly.kt")
    public void testInlineOnly() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/inlineOnly.kt");
    }

    @Test
    @TestMetadata("inlineReified.kt")
    public void testInlineReified() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/inlineReified.kt");
    }

    @Test
    @TestMetadata("jvmField.kt")
    public void testJvmField() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/jvmField.kt");
    }

    @Test
    @TestMetadata("jvmName.kt")
    public void testJvmName() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/jvmName.kt");
    }

    @Test
    @TestMetadata("jvmOverloads.kt")
    public void testJvmOverloads() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/jvmOverloads.kt");
    }

    @Test
    @TestMetadata("jvmRecord.kt")
    public void testJvmRecord() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/jvmRecord.kt");
    }

    @Test
    @TestMetadata("jvmSynthetic.kt")
    public void testJvmSynthetic() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/jvmSynthetic.kt");
    }

    @Test
    @TestMetadata("jvmSyntheticForAccessors.kt")
    public void testJvmSyntheticForAccessors() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/jvmSyntheticForAccessors.kt");
    }

    @Test
    @TestMetadata("jvmWildcardAnnotations.kt")
    public void testJvmWildcardAnnotations() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/jvmWildcardAnnotations.kt");
    }

    @Test
    @TestMetadata("lateinitProperty.kt")
    public void testLateinitProperty() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/lateinitProperty.kt");
    }

    @Test
    @TestMetadata("localClassDerived.kt")
    public void testLocalClassDerived() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/localClassDerived.kt");
    }

    @Test
    @TestMetadata("objects.kt")
    public void testObjects() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/objects.kt");
    }

    @Test
    @TestMetadata("properties.kt")
    public void testProperties() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/properties.kt");
    }

    @Test
    @TestMetadata("simpleFunctions.kt")
    public void testSimpleFunctions() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/simpleFunctions.kt");
    }

    @Test
    @TestMetadata("strangeIdentifiers.kt")
    public void testStrangeIdentifiers() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/strangeIdentifiers.kt");
    }

    @Test
    @TestMetadata("throwsAnnotation.kt")
    public void testThrowsAnnotation() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/throwsAnnotation.kt");
    }

    @Test
    @TestMetadata("typeAliases.kt")
    public void testTypeAliases() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/typeAliases.kt");
    }

    @Test
    @TestMetadata("typeAnnotations.kt")
    public void testTypeAnnotations() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/typeAnnotations.kt");
    }

    @Test
    @TestMetadata("valueClassInSignature.kt")
    public void testValueClassInSignature() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/valueClassInSignature.kt");
    }

    @Test
    @TestMetadata("wildcardOptimization.kt")
    public void testWildcardOptimization() throws Exception {
        runTest("compiler/testData/asJava/ultraLightClasses/wildcardOptimization.kt");
    }
}
