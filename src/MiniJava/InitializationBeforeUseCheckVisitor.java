package MiniJava;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import java.util.*;


public class InitializationBeforeUseCheckVisitor extends MiniJavaBaseVisitor<Set<Symbol>> {
    MiniJavaParser parser;
    final Map<String, Klass> klasses;
    ParseTreeProperty<Scope> scopes;
    Scope currentScope = null;


    public InitializationBeforeUseCheckVisitor(final Map<String, Klass> klasses, ParseTreeProperty<Scope> scopes, MiniJavaParser parser) {
        this.scopes = scopes;
        this.klasses = klasses;
        this.parser = parser;
    }

    @Override
    public Set<Symbol> visitClassDeclaration(MiniJavaParser.ClassDeclarationContext ctx) {
        enterScope(ctx);
        visitChildren(ctx);
        exitScope();
        return null;
    }

    @Override
    public Set<Symbol> visitMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx) {
        enterScope(ctx);
        visitChildren(ctx);
        exitScope();
        return null;
    }

    @Override
    public Set<Symbol> visitIfElseStatement(MiniJavaParser.IfElseStatementContext ctx) {
        visit(ctx.expression());
        Set<Symbol> initializedVariables = visit(ctx.ifBlock());
        initializedVariables.retainAll(visit(ctx.elseBlock()));
        for (Symbol sym : initializedVariables) {
            currentScope.initialize(sym);
        }
        return initializedVariables;
    }

    @Override
    public Set<Symbol> visitIfBlock(MiniJavaParser.IfBlockContext ctx) {
        enterScope(ctx);
        visitChildren(ctx);
        Set<Symbol> ifInit = currentScope.getInitializedVariables();
        exitScope();
        return ifInit;
    }

    @Override
    public Set<Symbol> visitElseBlock(MiniJavaParser.ElseBlockContext ctx) {
        enterScope(ctx);
        visitChildren(ctx);
        Set<Symbol> elseInit = currentScope.getInitializedVariables();
        exitScope();
        return elseInit;
    }

    @Override
    public Set<Symbol> visitWhileStatement(MiniJavaParser.WhileStatementContext ctx) {
        enterScope(ctx);
        visitChildren(ctx);
        exitScope();
        return null;
    }

    @Override
    public Set<Symbol> visitVariableAssignmentStatement(MiniJavaParser.VariableAssignmentStatementContext ctx) {
        Set<Symbol> sym = visitChildren(ctx);
        currentScope.initialize(currentScope.lookup(ctx.Identifier().getText()));
        return sym;
    }

    @Override
    public Set<Symbol> visitIdentifierExpression(MiniJavaParser.IdentifierExpressionContext ctx) {
        String identifier = ctx.Identifier().getText();
        if (!currentScope.hasBeenInitialized(identifier)) {
            ErrorStrategy.reportVariableMayNotHaveBeenInitializedError(parser, ctx.Identifier().getSymbol(), identifier);
        }
        return visitChildren(ctx);
    }

    public void enterScope(ParserRuleContext ctx) {
        currentScope = scopes.get(ctx);
    }

    private void exitScope() {
        currentScope = currentScope.getEnclosingScope();
    }
}
