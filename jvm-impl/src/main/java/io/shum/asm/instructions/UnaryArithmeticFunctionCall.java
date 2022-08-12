package io.shum.asm.instructions;

import org.objectweb.asm.MethodVisitor;

public final class UnaryArithmeticFunctionCall implements ArithmeticFunctionCall {
    public enum Operation {
        ABS, NEG, INCR, DECR
    }

    private final Operation operation;

    public UnaryArithmeticFunctionCall(Operation operation) {
        this.operation = operation;
    }

    @Override
    public void apply(MethodVisitor mv) {
        switch (operation) {
            case ABS -> executeOperation("abs", mv);
            case NEG -> executeOperation("negateExact", mv);
            case INCR -> executeOperation("incrementExact", mv);
            case DECR -> executeOperation("decrementExact", mv);
        }
    }

    private void executeOperation(String mathClassMethodName, MethodVisitor mv) {
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", mathClassMethodName, "(J)J", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
    }

    @Override
    public String toString() {
        return operation.name();
    }
}
