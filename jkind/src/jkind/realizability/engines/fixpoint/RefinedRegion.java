package jkind.realizability.engines.fixpoint;
import jkind.sexp.Sexp;

public class RefinedRegion {
    protected Sexp region;

    public RefinedRegion(Sexp region) {
        this.region = region;
    }

    public Sexp getRefinedRegion() { return region; }
//    protected String region;
//
//    public RefinedRegion(String region) {
//        this.region = region;
//    }
//
//    public String getRefinedRegion() { return region; }
}
