package jkind.aeval;


public class InvalidResult extends AevalResult {
    final private ValidSubset subset;

    public InvalidResult(ValidSubset subset) {
        this.subset = subset;
    }

    public InvalidResult() { this(null); }

    public ValidSubset getValidSubset() { return subset; }
}
