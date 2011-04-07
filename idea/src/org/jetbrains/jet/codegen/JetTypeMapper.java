package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.types.*;
import org.objectweb.asm.Type;

/**
 * @author yole
 */
public class JetTypeMapper {
    private final JetStandardLibrary standardLibrary;

    public JetTypeMapper(JetStandardLibrary standardLibrary) {
        this.standardLibrary = standardLibrary;
    }

    public Type mapType(final JetType jetType) {
        if (jetType.equals(JetStandardClasses.getUnitType())) {
            return Type.VOID_TYPE;
        }
        if (jetType.equals(standardLibrary.getIntType())) {
            return Type.INT_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getIntType()))) {
            return Type.getObjectType("java/lang/Integer");
        }
        if (jetType.equals(standardLibrary.getLongType())) {
            return Type.LONG_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getLongType()))) {
            return Type.getObjectType("java/lang/Long");
        }
        if (jetType.equals(standardLibrary.getShortType())) {
            return Type.SHORT_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getShortType()))) {
            return Type.getObjectType("java/lang/Short");
        }
        if (jetType.equals(standardLibrary.getByteType())) {
            return Type.BYTE_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getByteType()))) {
            return Type.getObjectType("java/lang/Byte");
        }
        if (jetType.equals(standardLibrary.getCharType())) {
            return Type.CHAR_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getCharType()))) {
            return Type.getObjectType("java/lang/Char");
        }
        if (jetType.equals(standardLibrary.getFloatType())) {
            return Type.FLOAT_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getFloatType()))) {
            return Type.getObjectType("java/lang/Float");
        }
        if (jetType.equals(standardLibrary.getDoubleType())) {
            return Type.DOUBLE_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getDoubleType()))) {
            return Type.getObjectType("java/lang/Double");
        }
        if (jetType.equals(standardLibrary.getBooleanType())) {
            return Type.BOOLEAN_TYPE;
        }
        if (jetType.equals(TypeUtils.makeNullable(standardLibrary.getBooleanType()))) {
            return Type.getObjectType("java/lang/Boolean");
        }
        if (jetType.equals(standardLibrary.getStringType()) || jetType.equals(standardLibrary.getNullableStringType())) {
            return Type.getType(String.class);
        }

        DeclarationDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (standardLibrary.getArray().equals(descriptor)) {
            if (jetType.getArguments().size() != 1) {
                throw new UnsupportedOperationException("arrays must have one type argument");
            }
            TypeProjection memberType = jetType.getArguments().get(0);
            Type elementType = mapType(memberType.getType());
            return Type.getType("[" + elementType.getDescriptor());
        }
        if (JetStandardClasses.getAny().equals(descriptor)) {
            return Type.getType(Object.class);
        }

        if (descriptor instanceof ClassDescriptor) {
            return Type.getObjectType(getFQName(descriptor).replace('.', '/'));
        }

        throw new UnsupportedOperationException("Unknown type " + jetType);
    }

    private static String getFQName(DeclarationDescriptor descriptor) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container != null && !(container instanceof ModuleDescriptor)) {
            return getFQName(container) + "." + descriptor.getName();
        }

        return descriptor.getName();
    }
}
