package jkind.realizability.writers;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jkind.aeval.SkolemRelation;
import jkind.lustre.Node;
import jkind.results.Counterexample;
import jkind.results.layout.RealizabilityNodeLayout;
import jkind.util.Util;

public class ExcelWriter extends Writer {
	private final jkind.writers.ExcelWriter internal;
	private final ConsoleWriter summaryWriter = new ConsoleWriter(null);
	private static final List<String> REALIZABLE_LIST = Collections.singletonList(Util.REALIZABLE);

	public ExcelWriter(String filename, Node node) {
		this.internal = new jkind.writers.ExcelWriter(filename, new RealizabilityNodeLayout(node));
	}

	@Override
	public void begin() {
		internal.begin();
	}

	@Override
	public void end() {
		internal.end();
	}

	@Override
	public void writeBaseStep(int k) {
		internal.writeBaseStep(REALIZABLE_LIST, k);
	}

	@Override
	public void writeRealizable(int k, double runtime) {
		internal.writeValid(REALIZABLE_LIST, "extend", k, runtime, runtime, Collections.emptyList(),
				Collections.emptySet(), Collections.emptyList(), false);
		summaryWriter.writeRealizable(k, runtime);
	}

	@Override
	public void writeUnrealizable(int k, List<String> conflicts, double runtime) {
		//internal.writeInvalid(Util.REALIZABLE, "base", k, conflicts, runtime);
		summaryWriter.writeUnrealizable(k, conflicts, runtime);
	}

	@Override
	public void writeUnrealizable(int k, List<String> conflicts, List<List<String>> diagnoses, double runtime) {
		summaryWriter.writeUnrealizable(k, conflicts, diagnoses, runtime);
	}

	@Override
	public void writeUnrealizable(Counterexample cex, List<String> conflicts, double runtime) {
		internal.writeInvalid(Util.REALIZABLE, "base", cex, conflicts, runtime);
		summaryWriter.writeUnrealizable(cex, conflicts, runtime);
	}

	@Override
	public void writeUnrealizable(int k, List<Counterexample> counterexamples, List<String> conflicts,
								  List<List<String>> diagnoses, double runtime) {
		//Leave blank for now.
		return;
	}

	@Override
	public void writeUnknown(int trueFor, Counterexample cex, double runtime) {
		Map<String, Counterexample> map = Collections.singletonMap(Util.REALIZABLE, cex);
		internal.writeUnknown(REALIZABLE_LIST, trueFor, map, runtime);
		summaryWriter.writeUnknown(trueFor, cex, runtime);
	}

	@Override
	public void writeInconsistent(int k, double runtime) {
		internal.writeInconsistent(Util.REALIZABLE, "base", k, runtime);
		summaryWriter.writeInconsistent(k, runtime);
	}

    @Override
    public void writeFixpointRealizable(int k, double runtime) {
        internal.writeValid(REALIZABLE_LIST, "fixpoint", k, runtime, runtime, Collections.emptyList(),
                Collections.emptySet(), Collections.emptyList(), false);
        summaryWriter.writeFixpointRealizable(k, runtime);
    }

	@Override
	public void writeFixpointRealizable(Counterexample cex, int k, double runtime) {
		summaryWriter.writeFixpointRealizable(cex, k, runtime);
	}

    @Override
    public void writeFixpointUnrealizable(int k, List<String> conflicts, double runtime) {
        //internal.writeInvalid(Util.REALIZABLE, "base", k, conflicts, runtime);
        summaryWriter.writeFixpointUnrealizable(k, conflicts, runtime);
    }
}