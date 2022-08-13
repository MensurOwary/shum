package io.shum.language;

import io.shum.asm.Context;
import io.shum.asm.instructions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {

    private int instructionPointer;
    private final List<Token> tokens;
    private final Context context;
    private final List<Instruction> instructions;

    public Parser(List<Token> tokens, Context context) {
        this.tokens = tokens;
        this.instructionPointer = 0;
        this.instructions = new ArrayList<>();
        this.context = context;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    private Token next() {
        var res = tokens.get(instructionPointer);
        instructionPointer++;
        return res;
    }

    private void prev() {
        instructionPointer--;
    }

    public void parse() {
        // instructions pointer
        while (instructionPointer < tokens.size()) {
            var token = next();

            var parsedInstruction = switch (token.tokenType()) {
                case MACRO -> {
                    var md = parseMacroDeclaration();
                    context.createNewMacroDeclaration(md);
                    yield md;
                }
                case FUNC -> {
                    var fd = processFuncDeclaration();
                    // remember user-defined functions and signatures
                    context.createNewFunctionDeclaration(fd);
                    yield fd;
                }
                case VALUE -> parseValue(token);
                case FUNCTION_INVOCATION -> DefaultFunctionCall.createFunctionCall(token.value(), context);
                default -> throw new IllegalStateException("Unexpected token: " + token.tokenType());
            };

            this.instructions.add(parsedInstruction);
        }
    }

    private MacroDeclaration parseMacroDeclaration() {
        var name = parseFunctionName();
        if (next().tokenType() != TokenType.EQUAL) {
            // TODO: better error messages needed
            throw new RuntimeException("'=' sign expected to define a macro body");
        }
        var instrToken = next();
        var macroInstructions = new ArrayList<Instruction>();
        while (instrToken.tokenType() != TokenType.END) {
            var instruction = parseCallableBodyInstruction(instrToken);
            macroInstructions.add(instruction);
            instrToken = next();
        }

        return new MacroDeclaration(name, macroInstructions);
    }

    private Instruction parseCallableBodyInstruction(Token instrToken) {
        return switch (instrToken.tokenType()) {
            case VALUE -> parseValue(instrToken);
            case FUNCTION_INVOCATION -> DefaultFunctionCall.createFunctionCall(instrToken.value(), context);
            default -> throw new IllegalStateException("Unexpected value: " + instrToken.tokenType());
        };
    }

    private FunctionDeclaration processFuncDeclaration() {
        var name = parseFunctionName();
        var functionReturns = false;
        int parameterCount = 0;
        var nextToken = next();
        if (nextToken.tokenType() == TokenType.VALUE) {
            if (Utils.isInteger(nextToken.value())) {
                parameterCount = Integer.parseInt(nextToken.value());
            } else {
                throw new RuntimeException("Function parameter count should be an integer. Given: " + nextToken.value());
            }
        } else {
            prev();
        }

        if (next().tokenType() != TokenType.EQUAL) {
            // TODO: better error messages needed
            throw new RuntimeException("'=' sign expected to define a function body");
        }
        var instrToken = next();
        var functionInstructions = new ArrayList<Instruction>();
        while (instrToken.tokenType() != TokenType.END) {
            if (instrToken.tokenType() == TokenType.RETURN) {
                functionReturns = true;
                instrToken = next();
                continue;
            }
            var instruction = parseCallableBodyInstruction(instrToken);

            functionInstructions.add(instruction);
            instrToken = next();
        }
        return new FunctionDeclaration(name, parameterCount, functionReturns, functionInstructions);
    }

    private String parseFunctionName() {
        var nameToken = next();
        if (Utils.isProperFunctionName(nameToken.value())) {
            return nameToken.value();
        }
        throw new RuntimeException("Function/macro name is not in a proper format. Does not match the pattern: [a-zA-Z_\\-][a-zA-Z_0-9\\-]*");
    }

    private Constant parseValue(Token token) {
        var tokenValue = token.value();
        if (Utils.isDoubleQuoted(tokenValue) || Utils.isSingleQuoted(tokenValue)) {
            return new Constant(ShumDataType.STRING, tokenValue.substring(1, tokenValue.length() - 1));
        }

        if (Utils.isInteger(tokenValue)) {
            return new Constant(ShumDataType.INT, tokenValue);
        }

        if (Utils.isFloatingPoint(tokenValue)) {
            return new Constant(ShumDataType.DOUBLE, tokenValue);
        }

        throw new RuntimeException(String.format("Token '%s' is unknown", tokenValue));
    }

}
