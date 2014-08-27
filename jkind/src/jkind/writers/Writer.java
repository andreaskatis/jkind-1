package jkind.writers;

import java.util.List;
import java.util.Map;

import jkind.invariant.Invariant;
import jkind.results.Counterexample;

public abstract class Writer {
	public abstract void begin();

	public abstract void end();

	public abstract void writeValid(List<String> props, int k, double runtime,
			List<Invariant> invariants);

	public abstract void writeInvalid(String prop, Counterexample cex, double runtime);

	public abstract void writeUnknown(List<String> props, int trueFor,
			Map<String, Counterexample> inductiveCounterexamples, double runtime);
	
	public abstract void writeValidRealizability(List<String> reals, int k, double runtime,
			List<Invariant> invariants);
	
	public abstract void writeInvalidRealizability(String real, Counterexample cex, double runtime);
	
	public abstract void writeUnknownRealizabilities(List<String> reals, int trueFor, Map<String, Counterexample> inductiveCounterexamples, double runtime);
}
