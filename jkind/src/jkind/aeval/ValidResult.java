package jkind.aeval;

public class ValidResult extends AevalResult {
    final private SkolemRelation skolem;

    public ValidResult(SkolemRelation skolem) {
        super();
        this.skolem = skolem;
    }

    public ValidResult() {
        this(null);
    }

    public SkolemRelation getSkolem() {
        return skolem;
    }
}
