package jkind.realizability.engines.messages;

import java.util.HashMap;
import java.util.List;

import jkind.engines.invariant.ListInvariant;
import jkind.lustre.Node;
import jkind.solvers.Model;
import jkind.util.Util;

public class UnrealizableMessage extends Message {
	public final int k;
	public final Model model;
	public final List<String> properties;
	public final List<List<String>> diagnoses;
	public final HashMap<List<String>, Model> models;
	public final HashMap<List<String>, Integer> cexLengths;
	public final HashMap<String, Node> dependencies;

	public UnrealizableMessage(int k, Model model, List<String> conflicts) {
		this.k = k;
		this.model = model;
		this.properties = Util.safeList(conflicts);
		this.diagnoses = null;
		this.models = null;
		this.cexLengths = null;
		this.dependencies = null;
	}

	public UnrealizableMessage(int k, List<String> conflicts, List<List<String>> diagnoses) {
		this.k = k;
		this.model = null;
		this.properties = Util.safeList(conflicts);
		this.diagnoses = Util.safeList(diagnoses);
		this.models = null;
		this.cexLengths = null;
		this.dependencies = null;
	}

	public UnrealizableMessage(int k, List<String> conflicts, List<List<String>> diagnoses,
							   HashMap<List<String>, Model> models, HashMap<List<String>, Integer> cexLengths,
							   HashMap<String, Node> dependencies) {
		this.k = k;
		this.model = null;
		this.properties = Util.safeList(conflicts);
		this.diagnoses = Util.safeList(diagnoses);
		this.models = models;
		this.cexLengths = cexLengths;
		this.dependencies = dependencies;
	}
}
