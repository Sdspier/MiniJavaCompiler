package MiniJava;

import java.util.*;

/**
 * An interface for defining scopes and scoping rules
 */
public interface Scope {

    String getScopeName();

    Scope getEnclosingScope();

    void define(Symbol sym);

    void initialize(Symbol sym);

    Symbol lookup(String name);

    Symbol lookupLocally(String name);

    boolean hasBeenInitialized(String name);

    Set<Symbol> getInitializedVariables();

    /**
     * @param scope A Scope for which you want to know the Klass that contains it.
     * @return the Klass that contains scope.
     * If a Method m is overwritten in a subclass getEnclosingKlass(m) returns the subclass
     */
    static Klass getEnclosingKlass(Scope scope) {
        while (!(scope instanceof Klass)) {
            scope = scope.getEnclosingScope();
        }
        return (Klass) scope;//The outermost scope will always be a class.
    }

    static Method getEnclosingMethod(Scope scope) {
        while (!(scope instanceof Method)) {
            scope = scope.getEnclosingScope();
        }
        return (Method) scope;
    }
}
