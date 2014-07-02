package jkindreal.processes.messages;

import java.util.List;

import jkind.results.Counterexample;

public class CounterexampleMessageReal extends MessageReal {
	final public List<String> invalid;
	final public Counterexample cex;

	public CounterexampleMessageReal(List<String> invalid, Counterexample cex) {
		this.invalid = invalid;
		this.cex = cex;
	}
}
