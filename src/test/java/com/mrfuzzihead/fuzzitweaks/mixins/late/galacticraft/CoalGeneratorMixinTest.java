package com.mrfuzzihead.fuzzitweaks.mixins.late.galacticraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Bytecode-level guard for the Galacticraft fuel-consumption redirect. Loading the mixin normally would require a
 * Galacticraft runtime, so this reads the compiled mixin class directly and checks the injection contract instead.
 */
class CoalGeneratorMixinTest {

    private static final String MIXIN_RESOURCE = "com/mrfuzzihead/fuzzitweaks/mixins/late/galacticraft/TileEntityCoalGeneratorMixin.class";

    @Test
    void fuelHandlerKeepsItsRedirectAndBurnTimerWrite() throws Exception {
        final ClassNode mixin = readMixin();
        final MethodNode consumeFuel = findMethod(mixin, "fuzziTweaks$consumeFuel");
        assertNotNull(consumeFuel, "fuel-consumption handler is missing");

        final AnnotationNode redirect = findAnnotation(consumeFuel, "Lorg/spongepowered/asm/mixin/injection/Redirect;");
        assertNotNull(redirect, "fuel-consumption handler is not a @Redirect");
        assertEquals("updateEntity", annotationString(redirect, "method"));

        final AnnotationNode at = (AnnotationNode) annotationValue(redirect, "at");
        assertNotNull(at, "@Redirect has no @At");
        assertEquals("INVOKE", annotationString(at, "value"));

        final String target = annotationString(at, "target");
        assertNotNull(target);
        assertTrue(
            target.contains("decrStackSize(II)Lnet/minecraft/item/ItemStack;"),
            "fuel redirect no longer targets decrStackSize: " + target);
        assertTrue(writesItemCookTime(consumeFuel), "fuel handler no longer schedules a burn time");
    }

    private static ClassNode readMixin() throws Exception {
        try (InputStream input = CoalGeneratorMixinTest.class.getClassLoader()
            .getResourceAsStream(MIXIN_RESOURCE)) {
            assertNotNull(input, "compiled mixin class was not on the test classpath");
            final ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static MethodNode findMethod(ClassNode node, String name) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name)) {
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
        if (annotation.values == null) {
            return null;
        }
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (key.equals(annotation.values.get(i))) {
                return annotation.values.get(i + 1);
            }
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

    private static boolean writesItemCookTime(MethodNode method) {
        for (AbstractInsnNode instruction = method.instructions.getFirst(); instruction
            != null; instruction = instruction.getNext()) {
            if (instruction.getOpcode() == Opcodes.PUTFIELD && instruction instanceof FieldInsnNode) {
                final FieldInsnNode field = (FieldInsnNode) instruction;
                if ("itemCookTime".equals(field.name) && "I".equals(field.desc)) {
                    return true;
                }
            }
        }
        return false;
    }
}
