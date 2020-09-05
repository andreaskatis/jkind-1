package jkind.aeval;

public class ValidResult extends AevalResult {
    final private SkolemFunction skolem;

    public ValidResult(SkolemFunction skolem) {
        super();
        this.skolem = skolem;
    }

    public ValidResult() {
        this(null);
    }

    public String getSkolem() {
        return skolem.getSkolemRelation();
    }
}
