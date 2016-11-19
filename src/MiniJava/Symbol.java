package MiniJava;

public class Symbol {
    String name;
    Klass type;

    public Symbol(String name) {
        this.name = name;
    }

    public Symbol(String name, Klass type, boolean isField) {
        this(name);
        this.type = type;
    }

    public Klass getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        if (type != null) return '<' + getName() + ":" + type + '>';
        return getName();
    }
}
