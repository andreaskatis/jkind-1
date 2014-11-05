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
import jkind.results.Property;
import jkind.results.UnknownProperty;
import jkind.results.ValidProperty;
import jkind.results.layout.NodeLayout;

public class ExcelWriter extends Writer {
	final private File file;
	final private Node node;
	final private List<Property> properties = new ArrayList<>();
	private ExcelFormatter formatter;

	public ExcelWriter(String filename, Node node) {
		this.file = new File(filename);
		this.node = node;
	}

	@Override
	public void begin() {
		formatter = new ExcelFormatter(file, new NodeLayout(node));
	}

	@Override
	public void end() {
		formatter.write(properties);
		formatter.close();
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
	public void writeUnknown(List<String> props, int trueFor,
			Map<String, Counterexample> inductiveCounterexamples, double runtime) {
		for (String prop : props) {
			properties.add(new UnknownProperty(prop, trueFor, inductiveCounterexamples.get(prop),
					runtime));
		}
	}

	@Override
	public void writeBaseStep(List<String> props, int k) {
	}
}
