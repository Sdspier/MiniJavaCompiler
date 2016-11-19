package MiniJava;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.*;

public class AssignmentListener extends MiniJavaBaseListener {
    MiniJavaParser parser;
    final Map<String, Klass> klasses;
    ParseTreeProperty<Scope> scopes;
    Scope currentScope = null;
    boolean isField;

    public AssignmentListener(final Map<String, Klass> klasses, ParseTreeProperty<Scope> scopes, MiniJavaParser parser) {
        this.scopes = scopes;
        this.klasses = klasses;
        this.parser = parser;
    }

    private void saveScope(ParserRuleContext ctx, Scope s) {
        scopes.put(ctx, s);
    }

    @Override
    public void enterMainClass(MiniJavaParser.MainClassContext ctx) {
        Klass klass = klasses.get(ctx.Identifier(0).getText());
        currentScope = klass;
        saveScope(ctx, currentScope);
    }

    @Override
    public void enterClassDeclaration(MiniJavaParser.ClassDeclarationContext ctx) {
        Klass klass = klasses.get(ctx.Identifier(0).getText());
        currentScope = klass;
        saveScope(ctx, currentScope);
        Klass superKlass;
        if (ctx.Identifier().size() > 1) {
            superKlass = klasses.get(ctx.Identifier(1).getText());
            if (superKlass == null) {
                ErrorStrategy.reportFileNameAndLineNumber(ctx.Identifier(1).getSymbol());
                System.err.println("error: cannot find symbol.");
                ErrorStrategy.reportUnderlineError(parser, ctx.Identifier(1).getSymbol());
                System.err.println("symbol:   class " + ctx.Identifier(1).getText());
            }
        } else {
            superKlass = null;
        }
        klass.superKlass = superKlass;
        ErrorStrategy.reportCyclicInheritanceError(parser, ctx, klass);
    }

    @Override
    public void exitClassDeclaration(MiniJavaParser.ClassDeclarationContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void enterFieldDeclaration(MiniJavaParser.FieldDeclarationContext ctx) {
        isField = true;
    }

    @Override
    public void exitFieldDeclaration(MiniJavaParser.FieldDeclarationContext ctx) {
        isField = false;
    }

    @Override
    public void enterVarDeclaration(MiniJavaParser.VarDeclarationContext ctx) {
        String typeName = ctx.type().getText();
        String varName = ctx.Identifier().getText();
        if (currentScope.lookupLocally(varName) != null) {
            ErrorStrategy.reportSymbolAlreadyDefinedError(parser, ctx.Identifier().getSymbol(), "variable", varName, currentScope.getScopeName());
        }
        currentScope.defineSymbolInCurrentScope(new Symbol(varName, klasses.get(typeName), isField));
    }

    @Override
    public void enterMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx) {
        Klass returnType = klasses.get(ctx.type().getText());
        if (returnType == null) {
            ErrorStrategy.reportFullError(parser, ctx.type().Identifier().getSymbol(),
                    "error: cannot find symbol.",
                    "symbol:   class " + ctx.type().getText(),
                    "location: class " + currentScope.getScopeName()
            );
        }
        String methodName = Method.getMethodSignature(ctx);
        if (currentScope.lookupLocally(methodName) != null) {
            ErrorStrategy.reportSymbolAlreadyDefinedError(parser, ctx.Identifier().getSymbol(), "method", methodName, currentScope.getScopeName());
        }
        Scope owner = currentScope;
        Method method = new Method(returnType, methodName, owner);
        currentScope.defineSymbolInCurrentScope(method);
        currentScope = method;
        saveScope(ctx, currentScope);
    }

    @Override
    public void exitMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override
    public void enterParameter(MiniJavaParser.ParameterContext ctx) {
        Klass parameterType = klasses.get(ctx.type().getText());
        Symbol parameter;
        if (parameterType == null) {
            ErrorStrategy.reportFullError(parser, ctx.type().Identifier().getSymbol(),
                    "error: cannot find symbol.",
                    "symbol:   class " + ctx.type().getText(),
                    "location: class " + currentScope.getEnclosingScope().getScopeName()
            );
        }
        //parameter is not a field
        parameter = new Symbol(ctx.Identifier().getText(), parameterType, false);
        ((Method) currentScope).addParameter(parameter);
    }

    @Override
    public void enterNestedStatement(MiniJavaParser.NestedStatementContext ctx) {
        enterScope(ctx);
    }

    @Override
    public void exitNestedStatement(MiniJavaParser.NestedStatementContext ctx) {
        exitScope();
    }

    @Override
    public void enterIfBlock(MiniJavaParser.IfBlockContext ctx) {
        enterScope(ctx);
    }

    @Override
    public void exitIfBlock(MiniJavaParser.IfBlockContext ctx) {
        exitScope();
    }

    @Override
    public void enterElseBlock(MiniJavaParser.ElseBlockContext ctx) {
        enterScope(ctx);
    }

    @Override
    public void exitElseBlock(MiniJavaParser.ElseBlockContext ctx) {
        exitScope();
    }

    @Override
    public void enterWhileStatement(MiniJavaParser.WhileStatementContext ctx) {
        enterScope(ctx);
    }

    @Override
    public void exitWhileStatement(MiniJavaParser.WhileStatementContext ctx) {
        exitScope();
    }

    public void enterScope(ParserRuleContext ctx) {
        Block explicitScope = new Block(currentScope);
        currentScope = explicitScope;
        saveScope(ctx, currentScope);
    }

    public void exitScope() {
        currentScope = currentScope.getEnclosingScope();
    }
}
