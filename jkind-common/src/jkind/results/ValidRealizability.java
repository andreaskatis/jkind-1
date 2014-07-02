package jkind.results;

import java.util.Collections;
import java.util.List;

public final class ValidRealizability extends Realizability {
	private final int k;
	private final double runtime;
	private final List<String> invariants;

	public ValidRealizability(String real, int k, double runtime, List<String> invariants) {
		super(real);
		this.k = k;
		this.runtime = runtime;
		this.invariants = invariants;
	}
	
	/**
	 * k value (from k-induction) used to prove the realizability
	 */
	public int getK() {
		return k;
	}

	/**
	 * Invariants used to prove realizability, only available if JKindApi.setReduceInvariants()
	 */
	public List<String> getInvariants() {
		return Collections.unmodifiableList(invariants);
	}

	/**
	 * Runtime of verification measured in seconds
	 */
	public double getRuntime() {
		return runtime;
	}
}
