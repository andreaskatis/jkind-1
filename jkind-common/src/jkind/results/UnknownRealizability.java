package jkind.results;



public final class UnknownRealizability extends Realizability {
	private final Counterexample cex;

	public UnknownRealizability(String name, Counterexample cex) {
		super(name);
		this.cex = cex;
	}

	public Counterexample getInductiveCounterexample() {
		return cex;
	}
}
