package MiniJava;


import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.*;
import java.util.*;


public class Main {

    private static String inputFile = null;

    public static void main(String[] args) throws IOException {

        //-------------------------------FILE I/O------------------------------------
        //---------------------------------------------------------------------------

        FileInputStream is = null;
        if (args.length > 0) {
            inputFile = args[0];
            is = new FileInputStream(inputFile);
        }


        System.err.println("\n-----" + getFileName() + "-----");
        //-------------------------------LEXER/PARSER SETUP--------------------------
        //---------------------------------------------------------------------------

        //Wrapper for FileInputStream
        ANTLRInputStream input = new ANTLRInputStream(is);

        //Lexer for tokenizing the FileInputStream
        MiniJavaLexer lexer = new MiniJavaLexer(input);

        //Stream view of tokenized file, built by lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        //Takes the token stream for parse tree construction
        MiniJavaParser parser = new MiniJavaParser(tokens);

        //Symbol-table representation of classes for use in semantic analysis
        Map<String, Klass> klasses = new HashMap<String, Klass>();

        //Collection of symbol-table scopes
        ParseTreeProperty<Scope> scopes = new ParseTreeProperty<Scope>();

        //Type of the LHS of a method call expression
        ParseTreeProperty<Klass> callerTypes = new ParseTreeProperty<Klass>();

        //Remove default error listeners and add custom listeners
        parser.removeErrorListeners();

        parser.addErrorListener(new DiagnosticErrorListener());
        parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);

        //Reports syntax errors upon construction of the parse tree.
        parser.addErrorListener(new UnderlineListener());

        //Construct the parse tree and report syntax errors
        ParseTree tree = parser.goal();
        //If errors were encountered during parsing, print them and stop compiling.
        ErrorStrategy.exitOnErrors();

        // print LISP-style tree
        // System.out.println(tree.toStringTree(parser));


        //-------------------------------STATIC SEMANTICS----------------------------
        //---------------------------------------------------------------------------

        //A listener for naming the classes in the symbol table
        ClassNamer namer = new ClassNamer(klasses, parser);
        //Walk the parse tree, creating klasses and naming them
        ParseTreeWalker.DEFAULT.walk(namer, tree);
        //If there were errors during naming (two classes with same name)
        ErrorStrategy.exitOnErrors();

        //Build the symbol Table
        AssignmentListener AListener = new AssignmentListener(klasses, scopes, parser);
        //ParseTreeWalker.PopulateSymbolTable()
        ParseTreeWalker.DEFAULT.walk(AListener, tree);
        //If there were errors during naming (Cyclic inheritance)
        ErrorStrategy.exitOnErrors();

        //Visitor for Type checking
        TypeCheckVisitor TCVisitor = new TypeCheckVisitor(klasses, scopes, callerTypes, parser);
        //ParseTreeWalker.TypeCheck()
        TCVisitor.visit(tree);
        //If there were errors during naming ( int x; x = 0; x = new int[] + x; )
        ErrorStrategy.exitOnErrors();

        //Visitor for ensuring variables are initialized before use
        InitializationBeforeUseCheckVisitor IBUCVisitor = new InitializationBeforeUseCheckVisitor(klasses, scopes, parser);
        //Walk the parse tree
        IBUCVisitor.visit(tree);
        //If there were errors during naming ( int x; x = x + 0; x not initialized )
        ErrorStrategy.exitOnErrors();

    }

    //Return the name of the file for System.err
    public static String getFileName() {
        File base = new File("C:/Users/Steve/IdeaProjects/MiniJavaCompiler/samples/errors/Semantic/");
        File file = new File(inputFile);
        String relativePath = base.toURI().relativize(file.toURI()).getPath();
        return relativePath;
    }

}