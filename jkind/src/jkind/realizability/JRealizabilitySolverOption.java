package jkind.realizability;

public enum JRealizabilitySolverOption {
    Z3, AEVAL;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
