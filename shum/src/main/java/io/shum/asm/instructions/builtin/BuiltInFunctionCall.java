package io.shum.asm.instructions.builtin;

import io.shum.asm.instructions.FunctionCall;

public sealed interface BuiltInFunctionCall extends FunctionCall
        permits ArithmeticFunctionCall, ComparisonFunctionCall, DropFunction, DupFunction, LogicalFunction, PrintFunction, StringFunction, SwapFunction { }
