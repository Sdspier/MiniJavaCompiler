package MiniJava;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.*;
import java.io.*;

/*
The ClassWriter class is a subclass of the ClassVisitor abstract class
that builds compiled classes directly in binary form. It produces as
output a byte array containing the compiled class, which can be retrieved
with the toByteArray method. It can be seen as an event consumer.
 */

public class BytecodeGenerator extends MiniJavaBaseListener implements Opcodes {

    public final org.objectweb.asm.commons.Method INIT() {
        return org.objectweb.asm.commons.Method.getMethod("void <init> ()");
    }

    MiniJavaParser parser;
    final Map<String, Klass> klasses;
    ParseTreeProperty<Scope> scopes;
    ParseTreeProperty<Klass> callerTypes;
    Scope currentScope = null;
    FileOutputStream fileOutputStream;
    Stack<Label> labelStack = new Stack<>();
    int argumentCount;

    ClassWriter cw;
    GeneratorAdapter mg;
    org.objectweb.asm.commons.Method currentMethod;

    public BytecodeGenerator(final Map<String, Klass> klasses, final ParseTreeProperty<Scope> scopes, final ParseTreeProperty<Klass> callerTypes, final MiniJavaParser parser) {
        this.klasses = klasses;
        this.scopes = scopes;
        this.callerTypes = callerTypes;
        this.parser = parser;
    }

    /* main class, enter*/
    @Override
    public void enterMainClass(MiniJavaParser.MainClassContext ctx) {
        Klass mainKlass = klasses.get(ctx.Identifier(0).getText());
        String mainKlassName = mainKlass.getScopeName();

        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_1, ACC_PUBLIC, mainKlassName, null, "java/lang/Object", null);

        mg = new GeneratorAdapter(ACC_PUBLIC, INIT(), null, null, cw);
        mg.loadThis();
        mg.invokeConstructor(Type.getType(Object.class), INIT());
        mg.returnValue();
        mg.endMethod();
        mg = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, org.objectweb.asm.commons.Method.getMethod("void main (String[])"), null, null, cw);
    }

    @Override
    public void exitMainClass(MiniJavaParser.MainClassContext ctx) {
        mg.returnValue();
        mg.endMethod();
        cw.visitEnd();
        Klass mainKlass = klasses.get(ctx.Identifier(0).getText());
        String mainKlassName = mainKlass.getScopeName();
        try {
            fileOutputStream = new FileOutputStream(new File(mainKlassName + ".class"));
            fileOutputStream.write(cw.toByteArray());
            fileOutputStream.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            System.exit(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }
    }

    /* classes */
    @Override
    public void enterClassDeclaration(MiniJavaParser.ClassDeclarationContext ctx) {
        enterScope(ctx);
        Klass klass = klasses.get(ctx.Identifier(0).getText());
        String klassName = klass.getScopeName();
        String superKlassName = klass.getSuperKlass() != null ? klass.getSuperKlass().getScopeName() : "java/lang/Object";

        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_1, ACC_PUBLIC, klassName, null, superKlassName, null);

        mg = new GeneratorAdapter(ACC_PUBLIC, INIT(), null, null, cw);
        mg.loadThis();
        mg.invokeConstructor(Type.getObjectType(superKlassName), INIT());
        mg.returnValue();
        mg.endMethod();

    }

    @Override
    public void exitClassDeclaration(MiniJavaParser.ClassDeclarationContext ctx) {
        cw.visitEnd();
        Klass klass = klasses.get(ctx.Identifier(0).getText());
        String klassName = klass.getScopeName();
        try {
            fileOutputStream = new FileOutputStream(new File(klassName + ".class"));
            fileOutputStream.write(cw.toByteArray());
            fileOutputStream.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            System.exit(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }
        exitScope();
    }

    /* local variables */
    @Override
    public void exitLocalDeclaration(MiniJavaParser.LocalDeclarationContext ctx) {
        Symbol var = currentScope.lookup(ctx.varDeclaration().Identifier().getText());
        Type type = var.getType().asAsmType();
        if (!var.hasLocalIdentifier()) {
            var.setLocalIdentifier(mg.newLocal(type));
        } else {
            System.out.println("Error: variable was declared twice.");
        }
    }

    @Override
    public void exitVarDeclaration(MiniJavaParser.VarDeclarationContext ctx) {
        Symbol variable = currentScope.lookup(ctx.Identifier().getText());
        if (variable.isField()) {
            cw.visitField(ACC_PROTECTED, variable.getName(), variable.getType().asAsmType().getDescriptor(),
                    null, null).visitEnd();
        }
    }

    /* methods */
    @Override
    public void enterMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx) {
        enterScope(ctx);
        Method methodRepresentation = (Method) currentScope;
        currentMethod = methodRepresentation.asAsmMethod();
        mg = new GeneratorAdapter(ACC_PUBLIC, currentMethod, null, null, cw);
    }

    @Override
    public void exitMethodDeclaration(MiniJavaParser.MethodDeclarationContext ctx) {
        mg.returnValue();
        mg.endMethod();
        exitScope();
    }

    /* parameters */
    @Override
    public void enterParameterList(MiniJavaParser.ParameterListContext ctx) {
        argumentCount = 0;
    }

    @Override
    public void enterParameter(MiniJavaParser.ParameterContext ctx) {
        currentScope.lookup(ctx.Identifier().getText()).setParameterIdentifier(argumentCount);
        argumentCount++;
    }

    /* printing */
    @Override
    public void enterPrintStatement(MiniJavaParser.PrintStatementContext ctx) {
        mg.getStatic(Type.getType(System.class), "out", Type.getType(PrintStream.class));
    }

    @Override
    public void exitPrintStatement(MiniJavaParser.PrintStatementContext ctx) {
        mg.invokeVirtual(Type.getType(PrintStream.class), org.objectweb.asm.commons.Method.getMethod("void println (int)"));
    }

    /* field and local variable assignments */
    @Override
    public void enterVariableAssignmentStatement(MiniJavaParser.VariableAssignmentStatementContext ctx) {
        Symbol variable = currentScope.lookup(ctx.Identifier().getText());
        if (variable.isField()) {
            mg.loadThis();
        }
    }

    @Override
    public void exitVariableAssignmentStatement(MiniJavaParser.VariableAssignmentStatementContext ctx) {
        Symbol variable = currentScope.lookup(ctx.Identifier().getText());
        Type type = variable.getType().asAsmType();
        if (variable.isField()) {
            //System.out.println("variable " + var.getName() + " is a field");
            Type owner = ((Klass) currentScope.getEnclosingScope()).asAsmType();
            mg.putField(owner, variable.getName(), type);
        } else if (variable.isParameter()) {
            mg.storeArg(variable.getParameterListIdentifier());
        } else {
            mg.storeLocal(variable.getLocalIdentifier(), type);
        }
    }

    /* array assignments */
    @Override
    public void enterArrayAssignmentStatement(MiniJavaParser.ArrayAssignmentStatementContext ctx) {
        Symbol variable = currentScope.lookup(ctx.Identifier().getText());
        Type type = variable.getType().asAsmType();
        if (variable.isField()) {
            Type owner = ((Klass) currentScope.getEnclosingScope()).asAsmType();
            mg.loadThis();
            mg.getField(owner, variable.getName(), type);
        } else if (variable.isParameter()) {
            mg.loadArg(variable.getParameterListIdentifier());
        } else {
            mg.loadLocal(variable.getLocalIdentifier(), type);
        }
    }

    @Override
    public void exitArrayAssignmentStatement(MiniJavaParser.ArrayAssignmentStatementContext ctx) {
        mg.arrayStore(Type.INT_TYPE);
    }

    /* if - else */
    @Override
    public void enterIfElseStatement(MiniJavaParser.IfElseStatementContext ctx) {
        Label enterElse = mg.newLabel();
        Label exitElse = mg.newLabel();
        labelStack.push(exitElse);
        labelStack.push(enterElse);
        labelStack.push(exitElse);
        labelStack.push(enterElse);
    }

    @Override
    public void enterIfBlock(MiniJavaParser.IfBlockContext ctx) {
        mg.ifZCmp(GeneratorAdapter.EQ, labelStack.pop());
    }

    @Override
    public void exitIfBlock(MiniJavaParser.IfBlockContext ctx) {
        mg.goTo(labelStack.pop());
    }

    @Override
    public void enterElseBlock(MiniJavaParser.ElseBlockContext ctx) {
        mg.mark(labelStack.pop());
    }

    @Override
    public void exitElseBlock(MiniJavaParser.ElseBlockContext ctx) {
        mg.mark(labelStack.pop());
    }

    /* while statements */
    @Override
    public void enterWhileStatement(MiniJavaParser.WhileStatementContext ctx) {
        Label enterWhile = mg.mark();
        Label exitWhile = mg.newLabel();
        labelStack.push(exitWhile);
        labelStack.push(enterWhile);
        labelStack.push(exitWhile);
    }

    @Override
    public void enterWhileBlock(MiniJavaParser.WhileBlockContext ctx) {
        mg.ifZCmp(GeneratorAdapter.EQ, labelStack.pop());
    }

    @Override
    public void exitWhileStatement(MiniJavaParser.WhileStatementContext ctx) {
        mg.goTo(labelStack.pop());
        mg.mark(labelStack.pop());
    }

    /* and */
    @Override
    public void exitAndExpression(MiniJavaParser.AndExpressionContext ctx) {
        mg.math(GeneratorAdapter.AND, Type.BOOLEAN_TYPE);
    }

    /* less-than */
    @Override
    public void exitLtExpression(MiniJavaParser.LtExpressionContext ctx) {
        Label trueLabel = mg.newLabel();
        Label endLabel = mg.newLabel();
        mg.ifCmp(Type.INT_TYPE, GeneratorAdapter.LT, trueLabel);
        mg.push(false);
        mg.goTo(endLabel);
        mg.mark(trueLabel);
        mg.push(true);
        mg.mark(endLabel);
    }

    /* addition */
    @Override
    public void exitAddExpression(MiniJavaParser.AddExpressionContext ctx) {
        mg.math(GeneratorAdapter.ADD, Type.INT_TYPE);
    }

    /* subtraction */
    @Override
    public void exitSubExpression(MiniJavaParser.SubExpressionContext ctx) {
        mg.math(GeneratorAdapter.SUB, Type.INT_TYPE);
    }

    /* multiplication */
    @Override
    public void exitMulExpression(MiniJavaParser.MulExpressionContext ctx) {
        mg.math(GeneratorAdapter.MUL, Type.INT_TYPE);
    }

    /* power */
    /*
    A label represents a position in bytecode of a method.
    Used for jump, goto, and switch
    Designates the instruction that is just after.
     */
    @Override
    public void exitPowExpression(MiniJavaParser.PowExpressionContext ctx) {
        Symbol base = new Symbol("base", klasses.get("int"), false);
        Type type = Type.INT_TYPE;
        base.setLocalIdentifier(mg.newLocal(type));
        Symbol pow = new Symbol("pow", klasses.get("int"), false);
        pow.setLocalIdentifier(mg.newLocal(type));
        mg.storeLocal(pow.getLocalIdentifier(), type);
        mg.storeLocal(base.getLocalIdentifier(), type);
        mg.push(1); //generate the instruction to push the given value on stack
        Label end = mg.newLabel();  //end position in bytecode
        Label loop = mg.mark(); //mark the current code position with a new label: LOOP START
        mg.loadLocal(pow.getLocalIdentifier(), type); // Gen instruction to load given local on the stack
        mg.ifZCmp(GeneratorAdapter.EQ, end); //Gen instr. to jump to label based on comparison of top integer stack to zero.
        mg.loadLocal(base.getLocalIdentifier(), type);
        mg.math(GeneratorAdapter.MUL, Type.INT_TYPE);
        mg.loadLocal(pow.getLocalIdentifier(), type);
        mg.push(1);
        mg.math(GeneratorAdapter.SUB, Type.INT_TYPE);
        mg.storeLocal(pow.getLocalIdentifier(), type);
        mg.goTo(loop); //Gen instr. to jump to loop if true: IF NOT FINISHED
        mg.mark(end);
    }

    /* array access */
    @Override
    public void exitArrayAccessExpression(MiniJavaParser.ArrayAccessExpressionContext ctx) {
        mg.arrayLoad(Type.INT_TYPE);
    }

    /* array.length() */
    @Override
    public void exitArrayLengthExpression(MiniJavaParser.ArrayLengthExpressionContext ctx) {
        mg.arrayLength();
    }

    /* method call */
    @Override
    public void exitMethodCallExpression(MiniJavaParser.MethodCallExpressionContext ctx) {
        Klass klass = callerTypes.get(ctx);
        mg.invokeVirtual(klass.asAsmType(), ((Method) klass.lookup(ctx.Identifier().getText() + "()")).asAsmMethod());
    }

    /* integer literal */
    @Override
    public void enterIntLitExpression(MiniJavaParser.IntLitExpressionContext ctx) {
        mg.push(Integer.parseInt(ctx.IntegerLiteral().getText()));
    }

    /* boolean literal */
    @Override
    public void enterBooleanLitExpression(MiniJavaParser.BooleanLitExpressionContext ctx) {
        boolean predicate = Boolean.parseBoolean(ctx.BooleanLiteral().getText());
        mg.push(predicate);
    }

    /* identifier */
    @Override
    public void exitIdentifierExpression(MiniJavaParser.IdentifierExpressionContext ctx) {
        Symbol var = currentScope.lookup(ctx.Identifier().getText());
        Type type = var.getType().asAsmType();
        if (var.isParameter()) {
            mg.loadArg(var.getParameterListIdentifier());
        } else if (var.isField()) {
            //System.out.println("Variable " + var.getName() + " is a field");
            Type owner = ((Klass) currentScope.getEnclosingScope()).asAsmType();
            mg.loadThis();
            mg.getField(owner, var.getName(), type);
        } else {
            mg.loadLocal(var.getLocalIdentifier(), type);
        }
    }

    /* this */
    @Override
    public void exitThisExpression(MiniJavaParser.ThisExpressionContext ctx) {
        mg.loadThis();
    }

    /* array instantiation */
    @Override
    public void exitArrayInstantiationExpression(MiniJavaParser.ArrayInstantiationExpressionContext ctx) {
        mg.newArray(Type.INT_TYPE);
    }

    /* object instantiation */
    @Override
    public void enterObjectInstantiationExpression(MiniJavaParser.ObjectInstantiationExpressionContext ctx) {
        Type type = Type.getObjectType(ctx.Identifier().getText());
        mg.newInstance(type);
        mg.dup();
        mg.invokeConstructor(type, INIT());
    }

    /* not */
    @Override
    public void exitNotExpression(MiniJavaParser.NotExpressionContext ctx) {
        mg.not();
    }

    /* scope */
    public void enterScope(ParserRuleContext ctx) {
        currentScope = scopes.get(ctx);
    }

    private void exitScope() {
        currentScope = currentScope.getEnclosingScope();
    }
}

/*
public class GeneratorAdapter extends LocalVariablesSorter
A MethodVisitor with convenient methods to generate code. For example, using this adapter, the class below

 public class Example {
     public static void main(String[] args) {
         System.out.println("Hello world!");
     }
 }

can be generated as follows:

 ClassWriter cw = new ClassWriter(true);
 cw.visit(V1_1, ACC_PUBLIC, "Example", null, "java/lang/Object", null);

 Method m = Method.getMethod("void <init> ()");
 GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, m, null, null, cw);
 mg.loadThis();
 mg.invokeConstructor(Type.getType(Object.class), m);
 mg.returnValue();
 mg.endMethod();

 m = Method.getMethod("void main (String[])");
 mg = new GeneratorAdapter(ACC_PUBLIC + ACC_STATIC, m, null, null, cw);
 mg.getStatic(Type.getType(System.class), "out", Type.getType(PrintStream.class));
 mg.push("Hello world!");
 mg.invokeVirtual(Type.getType(PrintStream.class), Method.getMethod("void println (String)"));
 mg.returnValue();
 mg.endMethod();

 cw.visitEnd();

 */
