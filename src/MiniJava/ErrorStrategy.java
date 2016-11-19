package MiniJava;

import org.antlr.v4.runtime.*;

public final class ErrorStrategy {
    private static boolean hasError = false;
    private static int errorCount = 0;

    public static boolean noErrors() {
        return !hasError;
    }

    public static void reportError() {
        hasError = true;
        errorCount++;
    }

    public static int getErrorCount() {
        return errorCount;
    }


    public static void exitOnErrors() {
        if (!ErrorStrategy.noErrors()) {
            System.err.println("[" + ErrorStrategy.getErrorCount() + "] errors found.\n\n");
            System.exit(1);
        }
    }

    public static void reportFileNameAndLineNumber(Token offendingToken) {
        reportError();
        System.err.print(Main.getFileName() + ":" + offendingToken.getLine() + ": ");

    }

    public static void reportFullError(Recognizer recognizer, Token offendingToken, String message, String symbol, String location) {
        ErrorStrategy.reportFileNameAndLineNumber(offendingToken);
        System.err.println(message);
        ErrorStrategy.reportUnderlineError(recognizer, offendingToken);
        System.err.println("  " + symbol);
        System.err.println("  " + location);
    }

    public static void reportIncompatibleReturnTypeError(Recognizer recognizer, Token offendingToken, Klass originalKlass, Klass overwritingKlass, Method originalMethod, Method overwritingMethod) {
        ErrorStrategy.reportFileNameAndLineNumber(offendingToken);
        System.err.println("error: " + overwritingMethod + " in class " + overwritingKlass + " cannot override " + originalMethod + " in class " + originalKlass);
        ErrorStrategy.reportUnderlineError(recognizer, offendingToken);
        System.err.println("return type " + overwritingMethod.getType() + " is not compatible with type " + originalMethod.getType());
    }

    public static void reportDuplicateClassError(Recognizer recognizer, Token offendingToken, String className) {
        ErrorStrategy.reportFileNameAndLineNumber(offendingToken);
        System.err.println("error: duplicate class: " + className);
        ErrorStrategy.reportUnderlineError(recognizer, offendingToken);
    }

    public static void reportVariableMayNotHaveBeenInitializedError(Recognizer recognizer, Token offendingToken, String symbolName) {
        ErrorStrategy.reportFileNameAndLineNumber(offendingToken);
        System.err.println("error: " + symbolName + " might not have been initialized");
        ErrorStrategy.reportUnderlineError(recognizer, offendingToken);
    }

    public static void reportSymbolAlreadyDefinedError(Recognizer recognizer, Token offendingToken, String symbolType, String symbol, String className) {
        ErrorStrategy.reportFileNameAndLineNumber(offendingToken);
        System.err.println("error: " + symbolType + " " + symbol + " already defined in class " + className);
        ErrorStrategy.reportUnderlineError(recognizer, offendingToken);
    }

    public static void reportRequiredFoundError(String message, Recognizer recognizer, Token offendingToken, String required, String found) {
        ErrorStrategy.reportFileNameAndLineNumber(offendingToken);
        System.err.println(message);
        ErrorStrategy.reportUnderlineError(recognizer, offendingToken);
        System.err.println("  required: " + required);
        System.err.println("  found:    " + found);
    }

    public static void reportBinaryOperatorTypeError(Recognizer recognizer, ParserRuleContext ctx, Token operator, Klass foundLeft, Klass foundRight, Klass expectedLeft, Klass expectedRight) {
        if (foundLeft != null && foundRight != null && !(foundLeft == expectedLeft && foundRight == expectedRight)) {
            ErrorStrategy.reportFileNameAndLineNumber(operator);
            System.err.println("error: bad operand types for binary operator '" + operator.getText() + "'");
            ErrorStrategy.reportUnderlineError(recognizer, operator);
            System.err.println("  first type:   " + foundLeft);
            System.err.println("  second type:  " + foundRight);
        }
    }

    public static void reportUnresolvedSymbolError(Recognizer recognizer, Token offendingToken, String symbolType, Klass location) {
        ErrorStrategy.reportFileNameAndLineNumber(offendingToken);
        System.err.println("error: cannot find symbol");
        ErrorStrategy.reportUnderlineError(recognizer, offendingToken);
        System.err.println("  symbol:   " + symbolType + " " + offendingToken.getText());
        System.err.println("  location: " + "class" + " " + location);

    }

    public static void reportUnderlineError(Recognizer recognizer, Token offendingToken) {
        int line = offendingToken.getLine();
        int charPositionInLine = offendingToken.getCharPositionInLine();
        CommonTokenStream tokens = (CommonTokenStream) recognizer.getInputStream();
        String input = tokens.getTokenSource().getInputStream().toString();
        String[] lines = input.split("\n");
        String errorLine = lines[line - 1];
        System.err.println(errorLine);
        for (int i = 0; i < charPositionInLine; i++) {
            if (errorLine.charAt(i) == '\t') {
                System.err.print("\t");
            } else {
                System.err.print(" ");
            }
        }
        int start = offendingToken.getStartIndex();
        int stop = offendingToken.getStopIndex();
        if (start >= 0 && stop >= 0) {
            for (int i = start; i <= stop; i++) System.err.print("^");
        }
        System.err.println();
    }

    public static void reportCyclicInheritanceError(Recognizer recognizer, MiniJavaParser.ClassDeclarationContext ctx, Klass klass) {
        Klass original = klass;
        while (klass != null) {
            klass = klass.getSuperKlass();
            if (klass == original) {
                ErrorStrategy.reportFileNameAndLineNumber(ctx.Identifier(1).getSymbol());
                System.err.println("error: cyclic inheritance.");
                ErrorStrategy.reportUnderlineError(recognizer, ctx.Identifier(1).getSymbol());
                System.exit(1);
            }
        }
    }
}
