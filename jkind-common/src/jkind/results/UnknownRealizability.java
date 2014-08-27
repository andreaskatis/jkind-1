package jkind.results;

/**
 * 
 * An unknown realizability
 *
 */


public final class UnknownRealizability extends Realizability {
	private final int trueFor;
	private final Counterexample cex;

	public UnknownRealizability(String name, int trueFor, Counterexample cex, double runtime) {
		super(name, runtime);
		this.trueFor = trueFor;
		this.cex = cex;
	}

	public Counterexample getInductiveCounterexample() {
		return cex;
	}
	
	/**
	 * How many steps the realizability was valid in the base case
	 */
	public int getTrueFor() {
		return trueFor;
	}
}
