package jkindreal.processes.messages;

import java.math.BigInteger;
import jkind.solvers.Model;

public class InductiveCounterexampleMessageReal extends MessageReal {
	final public String realizability;
	final public BigInteger n;
	final public int k;
	final public Model model;

	public InductiveCounterexampleMessageReal(String realizability, BigInteger n, int k, Model model) {
		this.realizability = realizability;
		this.n = n;
		this.k = k;
		this.model = model;
	}
}
