package jkindreal.processes.messages;

import java.util.List;

import jkind.solvers.Model;

public class InvalidRealizabilityMessage extends MessageReal {
	final public List<String> invalid;
	final public int k;
	final public Model model;

	public InvalidRealizabilityMessage(List<String> invalid, int k, Model model) {
		this.invalid = invalid;
		this.k = k;
		this.model = model;
	}
}
