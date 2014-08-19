package jkindreal.processes.messages;

import jkind.solvers.Model;

public class InductiveCounterexampleMessageReal extends MessageReal {
	final public String realizability;
	final public int k;
	final public Model model;

	public InductiveCounterexampleMessageReal(String realizability, int k, Model model) {
		this.realizability = realizability;
		this.k = k;
		this.model = model;
	}
}
