package jkindreal.processes.messages;

import java.util.List;

import jkind.invariant.Invariant;

public class ValidRealizabilityMessage extends MessageReal {
	final public List<String> valid;
	final public int k;
	final public List<Invariant> invariants;

	public ValidRealizabilityMessage(List<String> valid, int k, List<Invariant> invariants) {
		this.valid = valid;
		this.k = k;
		this.invariants = invariants;
	}
}
