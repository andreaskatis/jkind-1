package jkindreal.processes.messages;

import java.util.List;

import jkind.solvers.smtlib2.SmtLib2Model;

public class InvalidRealizabilityMessage extends MessageReal {
	final public List<String> invalid;
	final public int k;
	final public SmtLib2Model model;

	public InvalidRealizabilityMessage(List<String> invalid, int k, SmtLib2Model model) {
		this.invalid = invalid;
		this.k = k;
		this.model = model;
	}
}
