package jkind.slicing;

import java.util.List;
import java.util.Set;

import jkind.solvers.Model;
import jkind.solvers.SimpleModel;
import jkind.solvers.smtlib2.SmtLib2Model;
import jkind.util.StreamIndex;

public class ModelSlicer {
	public static Model slice(Model original, DependencySet keep) {
		SimpleModel sliced = new SimpleModel();
		for (String var : original.getVariableNames()) {
			StreamIndex si = StreamIndex.decode(var);
			if (si != null && keep.contains(si.getStream())) {
				sliced.addValue(si, original.getValue(var));
			}
		}
		return sliced;
	}
	
	public static SimpleModel slice_real(Model original, List<DependencySet> keep, List<String> inputs) {
		SimpleModel sliced = new SimpleModel();
		for(DependencySet k : keep) {
			if (k!=null) {
				for (String var : original.getVariableNames()) {
					StreamIndex si = StreamIndex.decode(var);
					if (si!=null && k.contains(si.getStream()) && (!sliced.getVariableNames().contains(si.getStream()))) {
						sliced.addValue(si, original.getValue(var));
					}
				}
			}
		for(String in : inputs) {
			for (String var: original.getVariableNames()) {
				StreamIndex si = StreamIndex.decode(var);
				if (si!=null && in.equals(si.getStream()) && (!sliced.getVariableNames().contains(si.getStream()))) {
					sliced.addValue(si, original.getValue(var));
				}
			}
		}
		}
		return sliced;
	}
}
