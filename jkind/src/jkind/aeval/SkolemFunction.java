package jkind.aeval;

public class SkolemFunction {
    protected String relation;

    public SkolemFunction(String relation) {
        this.relation = relation;
    }

    public String getSkolemRelation() { return relation; }
}
