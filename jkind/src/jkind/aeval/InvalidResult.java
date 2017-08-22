package jkind.aeval;


import jkind.solvers.Model;

public class InvalidResult extends AevalResult {
    final private ValidSubset subset;
    final private Model model;

    public InvalidResult(ValidSubset subset) {
        this.subset = subset;
        this.model = null;
    }

    public InvalidResult(ValidSubset subset, Model model) {
        this.subset = subset;
        this.model = model;
    }

    public InvalidResult() { this(null); }

    public String getValidSubset() { return subset.getValidSubset(); }
    public Model getModel() { return model; }
}
