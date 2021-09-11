package jkind.aeval;
import jkind.sexp.Sexp;

public class ValidSubset {
    protected Sexp subset;

    public ValidSubset(Sexp subset) {
        this.subset = subset;
    }

    public Sexp getValidSubset() { return subset; }

    //    protected String subset;
//
//    public ValidSubset(String subset) {
//        this.subset = subset;
//    }
//
//    public String getValidSubset() { return subset; }
}
