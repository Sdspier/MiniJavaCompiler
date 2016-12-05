package MiniJava;

public class Symbol {
    String name;
    Klass type;
    private boolean isField;
    private int localIdentifier = -1;
    private int parameterListIdentifier = -1;
    private boolean hasBeenInitialized = false;

    public void initialize() {
        hasBeenInitialized = true;
    }

    public boolean hasBeenInitialized() {
        return hasBeenInitialized;
    }


    public boolean isParameter() {
        return parameterListIdentifier != -1;
    }

    public void setParameterIdentifier(int parameterListIdentifier) {
        this.parameterListIdentifier = parameterListIdentifier;
    }

    public int getParameterListIdentifier() {
        return parameterListIdentifier;
    }

    public boolean isField() {
        return isField;
    }

    public boolean hasLocalIdentifier() {
        return localIdentifier != -1;
    }

    public int getLocalIdentifier() {
        assert localIdentifier >= 0;
        assert !isField;
        return localIdentifier;
    }

    public void setLocalIdentifier(int localIdentifier) {
        assert !isField;
        this.localIdentifier = localIdentifier;
    }

    public Symbol(String name, boolean isField) {
        this.name = name;
        this.isField = isField;
    }

    public Symbol(String name, Klass type, boolean isField) {
        this(name, isField);
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
