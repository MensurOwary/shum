package io.shum.asm.generation;

import io.shum.asm.instructions.FunctionDeclaration;
import io.shum.asm.instructions.Instruction;
import io.shum.asm.instructions.MacroDeclaration;
import org.objectweb.asm.ClassWriter;

import java.io.FileOutputStream;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ClassGenerator {

    private final String className;
    private final ClassWriter cw;

    public ClassGenerator(String className) {
        this.className = className;
        this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    }

    public void generate(List<Instruction> instructions) {
        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);

        var mg = new MethodGenerator(cw);

        instructions.stream()
                .filter(ins -> ins instanceof FunctionDeclaration)
                .forEach(ins -> mg.generate(((FunctionDeclaration) ins)));

        var immediatelyExecutedInstructions = instructions.stream()
                .filter(ins -> !(ins instanceof FunctionDeclaration || ins instanceof MacroDeclaration))
                .toList();

        mg.generate("main", "([Ljava/lang/String;)V", immediatelyExecutedInstructions);

        cw.visitEnd();
    }

    public void saveToFile() {
        try (var os = new FileOutputStream(String.format("%s.class", className))) {
            os.write(cw.toByteArray());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public Class<?> generateClass() {
        var bc = cw.toByteArray();

        return new ClassLoader(Thread.currentThread().getContextClassLoader()) {
            public Class<?> define(String className, byte[] bytecode) {
                return super.defineClass(className, bytecode, 0, bytecode.length);
            }
        }.define("Main", bc);
    }

    public static class DynamicClassLoader extends ClassLoader {
        public DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> define(String className, byte[] bytecode) {
            return super.defineClass(className, bytecode, 0, bytecode.length);
        }
    }

}
