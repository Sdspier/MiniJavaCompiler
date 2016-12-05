package MiniJava;

import java.util.*;

public class ClassNamer extends MiniJavaBaseListener {
    //The symbol-table collection of classes
    private Map<String, Klass> klasses;

    private MiniJavaParser parser;

    public ClassNamer(Map<String, Klass> klasses, MiniJavaParser parser) {
        this.klasses = klasses;
        this.parser = parser;
    }

    /**
     * Creates a klass in the symbol-table
     */
    @Override
    public void enterClassDeclaration(MiniJavaParser.ClassDeclarationContext ctx) {
        Klass currentKlass = new Klass(ctx.Identifier(0).getText());
        if (klasses.put(currentKlass.getScopeName(), currentKlass) != null) {
            ErrorStrategy.reportDuplicateClassError(parser, ctx.Identifier(0).getSymbol(), currentKlass.getScopeName());
        }
    }

    /**
     * Adds the main class along with the primitive types to the symbol-table
     */
    @Override
    public void enterMainClass(MiniJavaParser.MainClassContext ctx) {
        Klass currentKlass;

        currentKlass = new Klass("int[]");
        klasses.put(currentKlass.getScopeName(), currentKlass);

        currentKlass = new Klass("int");
        klasses.put(currentKlass.getScopeName(), currentKlass);

        currentKlass = new Klass("boolean");
        klasses.put(currentKlass.getScopeName(), currentKlass);

        currentKlass = new Klass(ctx.Identifier(0).getText());
        klasses.put(currentKlass.getScopeName(), currentKlass);
    }
}
