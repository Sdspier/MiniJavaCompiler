package MiniJava;

import org.antlr.v4.runtime.*;

public class UnderlineListener extends BaseErrorListener {

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        ErrorStrategy.reportFileNameAndLineNumber((Token) offendingSymbol);
        System.err.println("line " + line + ":" + charPositionInLine + " " + msg);
        ErrorStrategy.reportUnderlineError(recognizer, (Token) offendingSymbol
        );
    }
}
