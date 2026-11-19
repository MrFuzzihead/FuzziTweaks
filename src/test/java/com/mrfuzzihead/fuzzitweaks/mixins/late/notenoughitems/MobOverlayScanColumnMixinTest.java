package com.mrfuzzihead.fuzzitweaks.mixins.late.notenoughitems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Bytecode-level guard for the NEI mob-overlay threshold tweak.
 *
 * <p>
 * The mixin replaces the two {@code 8} literals of
 * {@code codechicken.nei.moboverlay.MobOverlayScan$Column#getSpawnMode(int)} by their ordinal, so it silently stops
 * working - and the overlay silently disagrees with the spawn rules again - once NEI reshapes that method. This test
 * pins both halves of the contract: the mixin's annotations, and the target method in the NEI version the build
 * compiles against.
 */
class MobOverlayScanColumnMixinTest {

    private static final String MIXIN_RESOURCE = "com/mrfuzzihead/fuzzitweaks/mixins/late/notenoughitems/MixinMobOverlayScanColumn.class";
    private static final String TARGET_RESOURCE = "codechicken/nei/moboverlay/MobOverlayScan$Column.class";
    private static final String MODIFY_CONSTANT = "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;";

    @Test
    void blockLightConstantIsTheFirstLiteral() throws Exception {
        final MethodNode handler = findHandler("fuzziTweaks$maxMobBlockLightLevel");
        assertEquals("getSpawnMode", annotationString(findAnnotation(handler, MODIFY_CONSTANT), "method"));
        assertConstantOrdinal(handler, 0);
    }

    @Test
    void skyLightConstantIsTheSecondLiteral() throws Exception {
        final MethodNode handler = findHandler("fuzziTweaks$maxMobSkyLightLevel");
        assertEquals("getSpawnMode", annotationString(findAnnotation(handler, MODIFY_CONSTANT), "method"));
        assertConstantOrdinal(handler, 1);
    }

    /**
     * The ordinals are only stable while {@code getSpawnMode} holds exactly those two {@code 8} literals, in the
     * block-light-then-sky-light order.
     */
    @Test
    void targetMethodStillHasExactlyTwoThresholdLiterals() throws Exception {
        final ClassNode target = readClass(TARGET_RESOURCE);
        final MethodNode getSpawnMode = findMethod(target, "getSpawnMode", "(I)B");
        assertNotNull(getSpawnMode, "MobOverlayScan$Column#getSpawnMode(I)B is gone; retarget the NEI mixin");

        assertEquals(
            2,
            countIntConstants(getSpawnMode, 8),
            "getSpawnMode no longer has exactly two block/sky light literals; the mixin ordinals are stale");
    }

    private static MethodNode findHandler(String name) throws Exception {
        final MethodNode handler = findMethod(readClass(MIXIN_RESOURCE), name, "(I)I");
        assertNotNull(handler, name + " handler is missing");
        return handler;
    }

    private static void assertConstantOrdinal(MethodNode handler, int expectedOrdinal) {
        final AnnotationNode modifyConstant = findAnnotation(handler, MODIFY_CONSTANT);
        assertNotNull(modifyConstant, "handler is not a @ModifyConstant");
        final AnnotationNode constant = firstAnnotation(annotationValue(modifyConstant, "constant"));
        assertNotNull(constant, "@ModifyConstant has no @Constant discriminator");
        assertEquals(8, annotationValue(constant, "intValue"), "the discriminator is not NEI's threshold literal");
        assertEquals(expectedOrdinal, annotationValue(constant, "ordinal"), "constant ordinal changed");
    }

    private static int countIntConstants(MethodNode method, int value) {
        int count = 0;
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction
            != null; instruction = instruction.getNext()) {
            if (instruction instanceof IntInsnNode
                && (instruction.getOpcode() == Opcodes.BIPUSH || instruction.getOpcode() == Opcodes.SIPUSH)) {
                if (((IntInsnNode) instruction).operand == value) {
                    count++;
                }
            } else if (instruction instanceof InsnNode) {
                if (instruction.getOpcode() == Opcodes.ICONST_0 + value) {
                    count++;
                }
            } else if (instruction instanceof LdcInsnNode) {
                if (Integer.valueOf(value)
                    .equals(((LdcInsnNode) instruction).cst)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static ClassNode readClass(String resource) throws Exception {
        try (InputStream input = MobOverlayScanColumnMixinTest.class.getClassLoader()
            .getResourceAsStream(resource)) {
            assertNotNull(input, resource + " was not on the test classpath");
            final ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    private static AnnotationNode findAnnotation(MethodNode method, String descriptor) {
        if (method.visibleAnnotations == null) {
            return null;
        }
        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (descriptor.equals(annotation.desc)) {
                return annotation;
            }
        }
        return null;
    }

    private static Object annotationValue(AnnotationNode annotation, String key) {
        if (annotation == null || annotation.values == null) {
            return null;
        }
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) {
                return annotation.values.get(i + 1);
            }
        }
        return null;
    }

    private static AnnotationNode firstAnnotation(Object value) {
        if (value instanceof AnnotationNode) {
            return (AnnotationNode) value;
        }
        // an annotation-valued member declared as an array is stored as a List of AnnotationNode
        if (value instanceof List && ((List<?>) value).size() == 1
            && ((List<?>) value).get(0) instanceof AnnotationNode) {
            return (AnnotationNode) ((List<?>) value).get(0);
        }
        return null;
    }

    private static String annotationString(AnnotationNode annotation, String key) {
        final Object value = annotationValue(annotation, key);
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List && ((List<?>) value).size() == 1) {
            return String.valueOf(((List<?>) value).get(0));
        }
        return null;
    }
}
