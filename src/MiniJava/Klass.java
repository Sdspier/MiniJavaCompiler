package MiniJava;

import java.util.*;

public class Klass implements Scope {
    public Klass superKlass;
    private String name;
    private Map<String, Symbol> symTable = new HashMap<String, Symbol>();

    public Klass(String name) {
        this.name = name;
    }

    public Klass getSuperKlass() {
        return this.superKlass;
    }

    @Override
    public String getScopeName() {
        return name;
    }

    /**
     * This is the highest level of scope, so return null.
     */
    @Override
    public Scope getEnclosingScope() {
        return null;
    }

    @Override
    public void defineSymbolInCurrentScope(Symbol sym) {
        symTable.put(sym.getName(), sym);
    }

    @Override
    public void initialize(Symbol sym) {
        assert false;
    }

    public boolean isInstanceOf(Klass other) {
        if (this.superKlass == null && other != this) {
            return false;
        } else if (other == this) {
            return true;
        } else {
            return this.superKlass.isInstanceOf(other);
        }
    }

    @Override
    public Symbol lookUpNameInContainingScope(String name) {
        Symbol symbol = null;
        for (Klass klass = this; symbol == null && klass != null; klass = klass.getSuperKlass()) {
            symbol = klass.symTable.get(name);
        }
        return symbol;
    }

    @Override
    public Symbol lookupLocally(String name) {
        return symTable.get(name);
    }

    @Override
    public boolean hasBeenInitialized(String name) {
        return this.lookUpNameInContainingScope(name) != null;
    }

    @Override
    public Set<Symbol> getInitializedVariables() {
        assert false;
        return null;
    }

    public String toString() {
        return name;
    }

}