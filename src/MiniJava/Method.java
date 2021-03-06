package MiniJava;

import java.util.*;
import java.util.stream.Collectors;


public class Method extends Symbol implements Scope {
    private LinkedHashMap<String, Symbol> parameters = new LinkedHashMap<>();
    private Scope owner;
    private Map<String, Symbol> locals = new HashMap<>();
    private Map<String, Symbol> initializedVariables = new HashMap<>();

    public Method(Klass returnType, String name, Scope owner) {
        super(name, returnType, true);
        this.owner = owner;
    }

    @Override
    public String getScopeName() { return this.name; }

    @Override
    public Scope getEnclosingScope() {
        return owner;
    }

    @Override
    public void define(Symbol sym) {
        locals.put(sym.getName(), sym);
    }

    @Override
    public void initialize(Symbol sym) {
        initializedVariables.put(sym.getName(), sym);
    }

    @Override
    public Symbol lookup(String name) {
        if (parameters.containsKey(name)) {
            return parameters.get(name);
        } else if (locals.containsKey(name)) {
            return locals.get(name);
        } else {
            return this.getEnclosingScope().lookup(name);
        }
    }

    @Override
    public Symbol lookupLocally(String name) {
        if (parameters.containsKey(name)) {
            return parameters.get(name);
        } else {
            return locals.get(name);
        }
    }

    @Override
    public boolean hasBeenInitialized(String name) {
        if (initializedVariables.containsKey(name) || parameters.containsKey(name)) {
            return true;
        } else {
            return this.getEnclosingScope().hasBeenInitialized(name);
        }
    }

    @Override
    public Set<Symbol> getInitializedVariables() {
        return new HashSet<>(this.initializedVariables.values());
    }

    public void addParameter(Symbol parameter) {
        parameters.put(parameter.getName(), parameter);
    }

    public List<Symbol> getParameterList() {
        return new ArrayList<>(parameters.values());
    }

    public List<Klass> getParameterListDefinition() {
        List<Symbol> parameterList = getParameterList();
        List<Klass> parameterListDefinition = parameterList.stream().map(Symbol::getType).collect(Collectors.toList());
        return parameterListDefinition;
    }

    public String toString() {
        return name;
    }

    public static String getMethodSignature(MiniJavaParser.MethodDeclarationContext ctx) {
        return ctx.Identifier().getText() + "()";
    }

    public String fullName() {
        String fullName = this.getType().toString() + " ";
        fullName += name;
        fullName = fullName.substring(0, fullName.length() - 1);
        boolean hasParameter = false;
        for (Symbol parameter : parameters.values()) {
            fullName += parameter.getType().getScopeName() + ", ";
            hasParameter = true;
        }
        if (hasParameter) {
            fullName = fullName.substring(0, fullName.length() - 2);
        }
        fullName += ")";
        //System.out.println(fullName);
        return fullName;
    }

    public org.objectweb.asm.commons.Method asAsmMethod() {
        /**    Returns an asm.commons.Method representation of this Method.*/
        return org.objectweb.asm.commons.Method.getMethod(this.fullName(), true);

    }
}