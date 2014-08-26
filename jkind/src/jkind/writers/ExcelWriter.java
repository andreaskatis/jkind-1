package jkind.writers;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jkind.excel.ExcelFormatter;
import jkind.invariant.Invariant;
import jkind.lustre.Node;
import jkind.results.Counterexample;
import jkind.results.InvalidProperty;
import jkind.results.InvalidRealizability;
import jkind.results.Property;
import jkind.results.Realizability;
import jkind.results.UnknownProperty;
import jkind.results.UnknownRealizability;
import jkind.results.ValidProperty;
import jkind.results.ValidRealizability;
import jkind.results.layout.NodeLayout;

public class ExcelWriter extends Writer {
	final private File file;
	final private Node node;
	final private List<Property> properties = new ArrayList<>();
	final private List<Realizability> realizabilities = new ArrayList<>();


	public ExcelWriter(String filename, Node node) {
		this.file = new File(filename);
		this.node = node;
	}

	@Override
	public void begin() {
	}

	@Override
	public void end() {
		try (ExcelFormatter formatter = new ExcelFormatter(file, new NodeLayout(node))) {
			formatter.write(properties);
		}
	}

	@Override
	public void writeValid(List<String> props, int k, double runtime, List<Invariant> invariants) {
		List<String> invariantsText = new ArrayList<>();
		for (Invariant invariant : invariants) {
			invariantsText.add(invariant.toString());
		}

		for (String prop : props) {
			properties.add(new ValidProperty(prop, k, runtime, invariantsText));
		}
	}

	@Override
	public void writeInvalid(String prop, Counterexample cex, double runtime) {
		properties.add(new InvalidProperty(prop, cex, runtime));
	}

	@Override
	public void writeUnknown(List<String> props,
			Map<String, Counterexample> inductiveCounterexamples) {
		for (String prop : props) {
			properties.add(new UnknownProperty(prop, inductiveCounterexamples.get(prop)));
		}
	}
	
	@Override
	public void writeValidRealizability(List<String> reals, int k, double runtime, List<Invariant> invariants) {
		List<String> invariantsText = new ArrayList<>();
		for (Invariant invariant : invariants) {
			invariantsText.add(invariant.toString());
		}

		for (String real : reals) {
			realizabilities.add(new ValidRealizability(real, k, runtime, invariantsText));
		}
	}

	@Override
	public void writeInvalidRealizability(String real, Counterexample cex, double runtime) {
		realizabilities.add(new InvalidRealizability(real, cex, runtime));
	}
	
	@Override
	public void writeUnknownRealizability(List<String> reals,
			Map<String, Counterexample> inductiveCounterexamples) {
		for (String real : reals) {
			realizabilities.add(new UnknownRealizability(real, inductiveCounterexamples.get(real)));
		}
	}
}
