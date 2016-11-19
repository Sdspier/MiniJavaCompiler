package MiniJava;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TypeCheckVisitor extends MiniJavaBaseVisitor<Klass> {
    final Map<String, Klass> klasses;
    ParseTreeProperty<Scope> scopes;
    Scope currentScope;
    ParseTreeProperty<Klass> callerTypes;
    MiniJavaParser parser;
    Klass INT;
    Klass INTARRAY;
    Klass BOOLEAN;

    public TypeCheckVisitor(final Map<String, Klass> klasses, ParseTreeProperty<Scope> scopes, ParseTreeProperty<Klass> callerTypes, MiniJavaParser parser) {
        INT = klasses.get("int");
        INTARRAY = klasses.get("int[]");
        BOOLEAN = klasses.get("boolean");
        this.klasses = klasses;
        this.scopes = scopes;
        this.callerTypes = callerTypes;
        this.parser = parser;
    }

    @Override
    public Klass visitMainClass(MiniJavaParser.MainClassContext ctx) {
        return scopeCheck(ctx);
    }

    @Override
    public Klass visitClassDeclaration(MiniJavaParser.ClassDeclarationContext ctx) {
        return scopeCheck(ctx);
    }

    @Override
    public Klass visitMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx) {
        currentScope = scopes.get(ctx);

        Klass originalKlass = ((Klass) (currentScope.getEnclosingScope())).getSuperKlass();
        Method originalMethod;
        if (originalKlass == null) {
            originalMethod = null;
        } else {
            originalMethod = (Method) originalKlass.lookUpNameInContainingScope(currentScope.getScopeName());
        }
        Method currentMethod = (Method) currentScope;
        Klass currentKlass = (Klass) currentMethod.getEnclosingScope();
        if (originalMethod != null && originalMethod.getType() != currentMethod.getType()) {
            ErrorStrategy.reportIncompatibleReturnTypeError(parser, ctx.Identifier().getSymbol(), originalKlass, currentKlass, originalMethod, currentMethod);
        }
        Klass result = visitChildren(ctx);
        currentScope = currentScope.getEnclosingScope();
        return null;
    }

    @Override
    public Klass visitMethodBody(MiniJavaParser.MethodBodyContext ctx) {
        for (MiniJavaParser.LocalDeclarationContext pCtx : ctx.localDeclaration()) {
            visit(pCtx);
        }
        for (MiniJavaParser.StatementContext pCtx : ctx.statement()) {
            visit(pCtx);
        }
        Klass formalReturnType = Scope.getEnclosingMethod(currentScope).getType();
        Klass actualReturnType = visit(ctx.expression());
        if (actualReturnType != null && !actualReturnType.isInstanceOf(formalReturnType)) {
            ErrorStrategy.reportRequiredFoundError(
                    "error: incompatible types.", parser, ctx.RETURN().getSymbol(), formalReturnType.toString(), actualReturnType.toString());
        }
        return null;
    }

    @Override
    public Klass visitType(MiniJavaParser.TypeContext ctx) {
        if (ctx.Identifier() != null) {
            String name = ctx.Identifier().getSymbol().getText();
            Klass var = klasses.get(name);
            if (var == null) {
                ErrorStrategy.reportUnresolvedSymbolError(parser, ctx.Identifier().getSymbol(), "class", Scope.getEnclosingKlass(currentScope));
            }
            return var;
        }
        return null;
    }

    @Override
    public Klass visitIfElseStatement(MiniJavaParser.IfElseStatementContext ctx) {
        Klass booleanExpression = visit(ctx.expression());
        visit(ctx.ifBlock());
        visit(ctx.elseBlock());
        if (booleanExpression != BOOLEAN) {
            ErrorStrategy.reportRequiredFoundError(
                    "error: incompatible types.", parser, ctx.LP().getSymbol(), BOOLEAN.toString(), booleanExpression.toString());
        }
        return null;
    }

    @Override
    public Klass visitWhileStatement(MiniJavaParser.WhileStatementContext ctx) {
        Klass booleanExpression = visit(ctx.expression());
        visit(ctx.whileBlock());
        if (booleanExpression != BOOLEAN) {
            ErrorStrategy.reportRequiredFoundError(
                    "error: incompatible types.", parser, ctx.LP().getSymbol(), BOOLEAN.toString(), booleanExpression.toString());
        }
        return null;
    }

    @Override
    public Klass visitPrintStatement(MiniJavaParser.PrintStatementContext ctx) {
        Klass printContents = visit(ctx.expression());
        if (printContents != null && printContents != INT) {
            ErrorStrategy.reportRequiredFoundError(
                    "error: incompatible types.", parser, ctx.LP().getSymbol(), INT.toString(), printContents.toString());
        }
        return null;
    }

    @Override
    public Klass visitVariableAssignmentStatement(MiniJavaParser.VariableAssignmentStatementContext ctx) {
        String name = ctx.Identifier().getSymbol().getText();
        Symbol var = currentScope.lookUpNameInContainingScope(name);
        Klass rightSide = visit(ctx.expression());
        if (var == null) {
            ErrorStrategy.reportUnresolvedSymbolError(parser, ctx.Identifier().getSymbol(), "variable", Scope.getEnclosingKlass(currentScope));
        } else if (rightSide != null && !rightSide.isInstanceOf(var.getType())) {
            ErrorStrategy.reportRequiredFoundError("error: incompatible types.", parser, ctx.Identifier().getSymbol(), var.getType().toString(), (rightSide.toString()));
        }
        return null;
    }

    @Override
    public Klass visitArrayAssignmentStatement(MiniJavaParser.ArrayAssignmentStatementContext ctx) {
        String name = ctx.Identifier().getSymbol().getText();
        Symbol var = currentScope.lookUpNameInContainingScope(name);
        Klass index = visit(ctx.expression(0));
        Klass rightSide = visit(ctx.expression(1));
        if (var == null) {
            ErrorStrategy.reportUnresolvedSymbolError(parser, ctx.Identifier().getSymbol(), "variable", Scope.getEnclosingKlass(currentScope));
        } else if (var.getType() != INTARRAY) {
            ErrorStrategy.reportRequiredFoundError("error: incompatible types.", parser, ctx.LSB().getSymbol(), INTARRAY.toString(), (var.getType().toString()));
        } else if (rightSide != null && INT != rightSide) {
            ErrorStrategy.reportRequiredFoundError("error: incompatible types.", parser, ctx.EQ().getSymbol(), INT.toString(), (rightSide.toString()));
        } else if (index != INT) {
            ErrorStrategy.reportRequiredFoundError("error: incompatible types.", parser, ctx.LSB().getSymbol(), INT.toString(), index.toString());
        }
        return null;
    }

    @Override
    public Klass visitAndExpression(MiniJavaParser.AndExpressionContext ctx) {
        ErrorStrategy.reportBinaryOperatorTypeError(parser, ctx, ctx.AND().getSymbol(), visit(ctx.expression(0)), visit(ctx.expression(1)), BOOLEAN, BOOLEAN);
        return BOOLEAN;
    }

    @Override
    public Klass visitLtExpression(MiniJavaParser.LtExpressionContext ctx) {
        ErrorStrategy.reportBinaryOperatorTypeError(parser, ctx, ctx.LT().getSymbol(), visit(ctx.expression(0)), visit(ctx.expression(1)), INT, INT);
        return BOOLEAN;
    }

    @Override
    public Klass visitAddExpression(MiniJavaParser.AddExpressionContext ctx) {
        ErrorStrategy.reportBinaryOperatorTypeError(parser, ctx, ctx.PLUS().getSymbol(), visit(ctx.expression(0)), visit(ctx.expression(1)), INT, INT);
        return INT;
    }

    @Override
    public Klass visitSubExpression(MiniJavaParser.SubExpressionContext ctx) {
        ErrorStrategy.reportBinaryOperatorTypeError(parser, ctx, ctx.MINUS().getSymbol(), visit(ctx.expression(0)), visit(ctx.expression(1)), INT, INT);
        return INT;
    }

    @Override
    public Klass visitMulExpression(MiniJavaParser.MulExpressionContext ctx) {
        ErrorStrategy.reportBinaryOperatorTypeError(parser, ctx, ctx.TIMES().getSymbol(), visit(ctx.expression(0)), visit(ctx.expression(1)), INT, INT);
        return INT;
    }

    @Override
    public Klass visitPowExpression(MiniJavaParser.PowExpressionContext ctx) {
        ErrorStrategy.reportBinaryOperatorTypeError(parser, ctx, ctx.POWER().getSymbol(), visit(ctx.expression(0)), visit(ctx.expression(1)), INT, INT);
        return INT;
    }

    @Override
    public Klass visitArrayAccessExpression(MiniJavaParser.ArrayAccessExpressionContext ctx) {
        Klass array = visit(ctx.expression(0));
        Klass index = visit(ctx.expression(1));
        if (array != INTARRAY) {
            ErrorStrategy.reportFileNameAndLineNumber(ctx.LSB().getSymbol());
            System.err.println("error: array required, but " + array + " found");
            ErrorStrategy.reportUnderlineError(parser, ctx.LSB().getSymbol());
        }
        if (index != INT) {
            ErrorStrategy.reportRequiredFoundError("error: incompatible type.", parser, ctx.LSB().getSymbol(), INT.toString(), index.toString());
        }
        return INT;
    }

    @Override
    public Klass visitArrayLengthExpression(MiniJavaParser.ArrayLengthExpressionContext ctx) {
        Klass intArr = visit(ctx.expression());
        if (intArr != INTARRAY) {
            ErrorStrategy.reportFileNameAndLineNumber(ctx.DOTLENGTH().getSymbol());
            System.err.println("error: bad operand type " + intArr + " for unary operator '.length'");
            ErrorStrategy.reportUnderlineError(parser, ctx.DOTLENGTH().getSymbol());
        }
        return INT;
    }

    @Override
    public Klass visitMethodCallExpression(MiniJavaParser.MethodCallExpressionContext ctx) {
        Klass type = visit(ctx.expression(0));
        callerTypes.put(ctx, type);
        if (type == null) {
            return null;
        }
        String methodName = ctx.Identifier().getText() + "()";
        Method method = (Method) (type.lookUpNameInContainingScope(methodName));
        if (method == null) {
            ErrorStrategy.reportUnresolvedSymbolError(parser, ctx.Identifier().getSymbol(), "method", type);
            return null;
        } else {
            List<Klass> parameterList = new ArrayList<Klass>();
            for (MiniJavaParser.ExpressionContext expCtx : ctx.expression().subList(1, ctx.expression().size())) {
                parameterList.add(visit(expCtx));
            }
            List<Klass> parameterListDefinition = method.getParameterListDefinition();
            if (parameterListDefinition.size() != parameterList.size()) {
                ErrorStrategy.reportRequiredFoundError(
                        "error: method call parameters of method " + method.getName() + " do not match method definition.",
                        parser, ctx.Identifier().getSymbol(), parameterListDefinition.toString(), parameterList.toString());
                System.err.println("reason: actual and formal argument lists differ in length.");
                return method.getType();
            }
            for (int i = 0; i < parameterListDefinition.size(); i++) {
                if (!parameterList.get(i).isInstanceOf(parameterListDefinition.get(i))) {
                    ErrorStrategy.reportRequiredFoundError(
                            "error: method call parameters of method " + method.getName() + " do not match method definition.",
                            parser, ctx.Identifier().getSymbol(), parameterListDefinition.toString(), parameterList.toString());
                }
            }
            return method.getType();
        }
    }

    @Override
    public Klass visitIntLitExpression(MiniJavaParser.IntLitExpressionContext ctx) {
        visitChildren(ctx);
        return INT;
    }

    @Override
    public Klass visitBooleanLitExpression(MiniJavaParser.BooleanLitExpressionContext ctx) {
        visitChildren(ctx);
        return BOOLEAN;
    }

    @Override
    public Klass visitIdentifierExpression(MiniJavaParser.IdentifierExpressionContext ctx) {
        String name = ctx.Identifier().getSymbol().getText();
        Symbol var = currentScope.lookUpNameInContainingScope(name);
        if (var == null) {
            ErrorStrategy.reportUnresolvedSymbolError(parser, ctx.Identifier().getSymbol(), "variable", Scope.getEnclosingKlass(currentScope));
            return null;
        }
        return var.getType();
    }

    @Override
    public Klass visitThisExpression(MiniJavaParser.ThisExpressionContext ctx) {
        visitChildren(ctx);
        return Scope.getEnclosingKlass(currentScope);
    }

    @Override
    public Klass visitArrayInstantiationExpression(MiniJavaParser.ArrayInstantiationExpressionContext ctx) {
        Klass type = visit(ctx.expression());
        if (type != INT) {
            ErrorStrategy.reportRequiredFoundError("error: incompatible types.", parser, ctx.LSB().getSymbol(), INT.toString(), type.toString());
        }
        return INTARRAY;
    }

    @Override
    public Klass visitObjectInstantiationExpression(MiniJavaParser.ObjectInstantiationExpressionContext ctx) {
        Klass type = klasses.get(ctx.Identifier().getText());
        if (type == null) {
            ErrorStrategy.reportUnresolvedSymbolError(parser, ctx.Identifier().getSymbol(), "class", Scope.getEnclosingKlass(currentScope));
        }
        return type;
    }

    @Override
    public Klass visitNotExpression(MiniJavaParser.NotExpressionContext ctx) {
        Klass bool = visit(ctx.expression());
        if (bool != BOOLEAN) {
            ErrorStrategy.reportFileNameAndLineNumber(ctx.NOT().getSymbol());
            System.err.println("error: bad operand type " + bool + " for unary operator '!'");
            ErrorStrategy.reportUnderlineError(parser, ctx.NOT().getSymbol());
        }
        return BOOLEAN;
    }

    @Override
    public Klass visitParenExpression(MiniJavaParser.ParenExpressionContext ctx) {
        return visit(ctx.expression());
    }

    public Klass scopeCheck(ParserRuleContext ctx) {
        currentScope = scopes.get(ctx);
        Klass result = visitChildren(ctx);
        currentScope = currentScope.getEnclosingScope();
        return null;
    }
}
