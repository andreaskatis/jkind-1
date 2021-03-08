package jkind.realizability.writers;

import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.results.Counterexample;
import jkind.util.Util;
import jkind.results.layout.Layout;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonWriter extends Writer {
    private final jkind.writers.JsonWriter internal;
    private final ConsoleWriter summaryWriter = new ConsoleWriter(null);
    private static final List<String> REALIZABLE_LIST = Collections.singletonList(Util.REALIZABLE);


    public JsonWriter(String filename, Map<String, Type> types, Layout layout) throws FileNotFoundException {
        this.internal = new jkind.writers.JsonWriter(filename, types, layout, false);
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
        return;
        //Do nothing for now. I don't think progress is necessary for unrealizable results
        //        internal.writeBaseStep(REALIZABLE_LIST, k);
    }

    @Override
    public void writeRealizable(int k, double runtime) {
        internal.writeValid("extend", k, runtime);
        summaryWriter.writeRealizable(k, runtime);
    }

    @Override
    public void writeUnrealizable(int k, List<String> conflicts, double runtime) {
//        internal.writeInvalid(Util.REALIZABLE, "base", cex, conflicts, runtime);
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
        internal.writeInvalid(Util.REALIZABLE, "base", counterexamples, conflicts, diagnoses, runtime);
        summaryWriter.writeUnrealizable(k, counterexamples, conflicts, diagnoses, runtime);
        return;
    }

    public void writeUnrealizable(int k, List<Counterexample> counterexamples, List<String> conflicts,
                                  List<List<String>> diagnoses, double runtime, HashMap<String, Node> dependencies) {
        internal.writeInvalid(Util.REALIZABLE, "base", counterexamples, conflicts, diagnoses, runtime, dependencies);
        summaryWriter.writeUnrealizable(k, counterexamples, conflicts, diagnoses, runtime);
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
        internal.writeValid("fixpoint", k, runtime);
        summaryWriter.writeFixpointRealizable(k, runtime);
    }

    @Override
    public void writeFixpointUnrealizable(int k, List<String> conflicts, double runtime) {
        //internal.writeInvalid(Util.REALIZABLE, "base", cex, conflicts, runtime);
        summaryWriter.writeFixpointUnrealizable(k, conflicts, runtime);
    }

}
