package jkind.realizability.engines.messages;

import jkind.solvers.Model;

public class RealizableMessage extends Message {
	public final int k;
	public final Model model;

	public RealizableMessage(int k) {
		this.k = k;
		this.model = null;
	}

	public RealizableMessage(int k, Model model) {
		this.k = k;
		this.model = model;
	}
}
