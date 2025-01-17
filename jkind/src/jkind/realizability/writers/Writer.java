package jkind.realizability.writers;

import java.util.ArrayList;
import java.util.List;

import jkind.aeval.SkolemRelation;
import jkind.results.Counterexample;

public abstract class Writer {
	public abstract void begin();

	public abstract void end();

	public abstract void writeBaseStep(int k);

	public abstract void writeRealizable(int k, double runtime);

	public abstract void writeUnrealizable(int k, List<String> conflicts,
										   double runtime);

	public abstract  void writeUnrealizable(int k, List<String> conflicts,
											List<List<String>> diagnoses, double runtime);

	public abstract void writeUnrealizable(Counterexample cex, List<String> conflicts,
			double runtime);

	public abstract void writeUnrealizable(int k, List<Counterexample> cexs, List<String> conflicts,
										   List<List<String>> diagnoses, double runtime);

	public abstract void writeUnknown(int trueFor, Counterexample cex, double runtime);

	public abstract void writeInconsistent(int k, double runtime);

    public abstract void writeFixpointRealizable(int k, double runtime);

	public abstract void writeFixpointRealizable(Counterexample cex, int k, double runtime);

    public abstract void writeFixpointUnrealizable(int k, List<String> conflicts, double runtime);
}
